package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.csv.CSVValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Interpreter extends QuestionAnswerAdaptor<Context, Value> {
    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);

    List<Value> evaluate(List<? extends PExp> list, Context ctxt) throws AnalysisException {
        List<Value> values = new Vector<>();
        for (PExp item : list) {
            values.add(item.apply(this, ctxt));
        }
        return values;
    }

    @Override
    public Value caseARootDocument(ARootDocument node, Context question) throws AnalysisException {

        return node.getContent().stream().filter(f -> f instanceof ASimulationSpecificationCompilationUnit).findFirst().map(spec -> {
            try {
                return spec.apply(this, question);
            } catch (AnalysisException e) {
                e.printStackTrace();
                return null;
            }
        }).orElse(null);

    }

    @Override
    public Value caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node,
            Context question) throws AnalysisException {
        return node.getBody().apply(this, question);

    }

    @Override
    public Value caseABlockStm(ABlockStm node, Context question) throws AnalysisException {

        for (PStm stm : node.getBody()) {
            stm.apply(this, question);
        }
        return new VoidValue();
    }

    @Override
    public Value caseALocalVariableStm(ALocalVariableStm node, Context question) throws AnalysisException {

        return node.getDeclaration().apply(this, question);
    }

    @Override
    public Value caseALoadExp(ALoadExp node, Context question) throws AnalysisException {

        if (node.getArgs().size() < 1) {
            throw new AnalysisException("load contains too few arguments. At least a type is required");
        }

        List<Value> args = evaluate(node.getArgs(), question);


        String type = ((StringValue) args.get(0)).getValue();
        if (type.equals("FMI2")) {

            String guid = ((StringValue) args.get(1)).getValue();
            String path = ((StringValue) args.get(2)).getValue();
            return new FmiInterpreter().createFmiValue(path, guid);
        } else if (type.equals("CSV")) {
            return new CSVValue();
        }
        throw new AnalysisException("Load of unknown type");
    }

    @Override
    public Value caseAUnloadExp(AUnloadExp node, Context question) throws AnalysisException {

        List<Value> args = evaluate(node.getArgs(), question);

        Value nameVal = args.get(0);
        if (nameVal instanceof FmuValue) {
            FmuValue fmuVal = (FmuValue) nameVal;
            FunctionValue unloadFunction = (FunctionValue) fmuVal.lookup("unload");
            return unloadFunction.evaluate(Collections.emptyList());
        } else if (nameVal instanceof CSVValue) {
            return new VoidValue();
        }
        throw new AnalysisException("UnLoad of unknown type: " + nameVal);
    }

    @Override
    public Value caseAAssigmentStm(AAssigmentStm node, Context question) throws AnalysisException {

        Value value = node.getExp().apply(this, question);

        if (node.getTarget() instanceof AIdentifierStateDesignator) {
            question.put(((AIdentifierStateDesignator) node.getTarget()).getName(), value);
            return new VoidValue();

        } else if (node.getTarget() instanceof AArrayStateDesignator) {
            AArrayStateDesignator arrayStateDesignator = (AArrayStateDesignator) node.getTarget();

            if (arrayStateDesignator.getTarget() instanceof AIdentifierStateDesignator) {

                AIdentifierStateDesignator designator = (AIdentifierStateDesignator) arrayStateDesignator.getTarget();

                Value arrayTarget = question.lookup(designator.getName());


                Value indexValue = arrayStateDesignator.getExp().apply(this, question);

                if (!(indexValue instanceof NumericValue)) {
                    throw new InterpreterException("Array index is not an integer: " + indexValue.toString());
                }

                int index = ((NumericValue) indexValue).intValue();


                if (arrayTarget.deref() instanceof ArrayValue) {

                    ArrayValue aval = (ArrayValue) arrayTarget.deref();

                    if (index >= 0 && index < aval.getValues().size()) {
                        //                        question.put(designator.getName(),newValue);
                        aval.getValues().set(index, value);
                        return new VoidValue();
                    } else {
                        throw new InterpreterException("Array index is out of bounds: " + index);
                    }
                }

            }


        }

        throw new InterpreterException("unsupported state designator");
    }

    @Override
    public Value caseAVariableDeclaration(AVariableDeclaration node, Context question) throws AnalysisException {


        if (node.getIsArray()) {

            Value val;
            if (node.getInitializer() != null) {
                val = node.getInitializer().apply(this, question);
            } else {

                if (node.getSize() == null || node.getSize().isEmpty()) {
                    throw new InterpreterException("Array size cannot be unspecified when no initializer is specified: " + node.getName());
                }

                //array deceleration
                NumericValue size = (NumericValue) node.getSize().get(0).apply(this, question);
                val = new ArrayValue<>(IntStream.range(0, size.intValue()).mapToObj(i -> new UndefinedValue()).collect(Collectors.toList()));
            }

            question.put(node.getName(), new ReferenceValue(val));

        } else {

            question.put(node.getName(), node.getInitializer() == null ? new UndefinedValue() : node.getInitializer().apply(this, question));
        }
        return new VoidValue();
    }

    @Override
    public Value caseAExpInitializer(AExpInitializer node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value caseAArrayInitializer(AArrayInitializer node, Context question) throws AnalysisException {


        ArrayValue<Value> array = new ArrayValue<>(evaluate(node.getExp(), question));
        return array;
    }

    @Override
    public Value caseADotExp(ADotExp node, Context question) throws AnalysisException {

        Value root = node.getRoot().apply(this, question);

        if (root instanceof ModuleValue) {

            return node.getExp().apply(this, new ModuleContext((ModuleValue) root, question));

        }
        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value caseAExpressionStm(AExpressionStm node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }


    @Override
    public Value caseAIdentifierExp(AIdentifierExp node, Context question) throws AnalysisException {
        return question.lookup(node.getName());
    }

    @Override
    public Value caseAPlusBinaryExp(APlusBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);


        if (left instanceof IntegerValue && right instanceof IntegerValue) {
            return new IntegerValue(left.intValue() + right.intValue());
        }

        return new RealValue(left.realValue() + right.realValue());
    }


    @Override
    public Value caseAMinusBinaryExp(AMinusBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);


        if (left instanceof IntegerValue && right instanceof IntegerValue) {
            return new IntegerValue(left.intValue() + right.intValue());
        }

        return new RealValue(left.realValue() - right.realValue());
    }

    @Override
    public Value caseACallExp(ACallExp node, Context question) throws AnalysisException {

        Value function = node.getRoot().apply(this, question);

        if (function instanceof FunctionValue) {

            return ((FunctionValue) function).evaluate(evaluate(node.getArgs(), question));
        }

        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value caseAWhileStm(AWhileStm node, Context question) throws AnalysisException {

        while (((BooleanValue) node.getTest().apply(this, question)).getValue()) {
            node.getBody().apply(this, question);
        }
        return new VoidValue();

    }

    @Override
    public Value caseAIfStm(AIfStm node, Context question) throws AnalysisException {
        if (((BooleanValue) node.getTest().apply(this, question)).getValue()) {
            node.getThen().apply(this, question);
        } else if (node.getElse() != null) {
            node.getElse().apply(this, question);
        }
        return new VoidValue();
    }

    @Override
    public Value caseALessBinaryExp(ALessBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);

        return new BooleanValue(left.compareTo(right) < 0);
    }

    @Override
    public Value caseALessEqualBinaryExp(ALessEqualBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);

        return new BooleanValue(left.compareTo(right) <= 0);
    }

    @Override
    public Value caseAGreaterBinaryExp(AGreaterBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);

        return new BooleanValue(left.compareTo(right) > 0);
    }

    @Override
    public Value caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question);
        NumericValue right = (NumericValue) node.getRight().apply(this, question);

        return new BooleanValue(left.compareTo(right) >= 0);
    }

    @Override
    public Value caseARealLiteralExp(ARealLiteralExp node, Context question) throws AnalysisException {
        return new RealValue(node.getValue());
    }

    @Override
    public Value caseABoolLiteralExp(ABoolLiteralExp node, Context question) throws AnalysisException {
        return new BooleanValue(node.getValue());
    }

    @Override
    public Value caseAStringLiteralExp(AStringLiteralExp node, Context question) throws AnalysisException {
        return new StringValue(node.getValue());
    }

    @Override
    public Value caseAIntLiteralExp(AIntLiteralExp node, Context question) throws AnalysisException {
        return new IntegerValue(node.getValue());
    }

    @Override
    public Value caseAUIntLiteralExp(AUIntLiteralExp node, Context question) throws AnalysisException {
        //TODO
        return new IntegerValue(node.getValue().intValue());
    }

    @Override
    public Value caseAArrayIndexExp(AArrayIndexExp node, Context question) throws AnalysisException {
        Value value = node.getArray().apply(this, question).deref();

        if (value instanceof ArrayValue) {
            ArrayValue<Value> array = (ArrayValue<Value>) value;

            List<NumericValue> indies = evaluate(node.getIndices(), question).stream().map(NumericValue.class::cast).collect(Collectors.toList());


            return array.getValues().get(indies.get(0).intValue());
        }
        throw new AnalysisException("No array or index for: " + node);
    }

    @Override
    public Value createNewReturnValue(INode node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter node: {}", node.getClass().getSimpleName());
        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value createNewReturnValue(Object node, Context question) throws AnalysisException {
        throw new InterpreterException("Unhandled node: " + node);
    }
}
