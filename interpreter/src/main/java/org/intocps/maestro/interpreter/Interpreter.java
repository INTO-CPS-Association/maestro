package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.interpreter.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        List<Value> args = evaluate(node.getArgs(), question);

        String type = ((StringValue) args.get(0)).getValue();
        String guid = ((StringValue) args.get(1)).getValue();
        String path = ((StringValue) args.get(2)).getValue();

        if (type.equals("FMI2")) {

            return new FmiInterpreter().createFmiValue(path, guid);
        }
        throw new AnalysisException("Load of unknown type");
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

                if (!(indexValue instanceof IntegerValue)) {
                    throw new InterpreterException("Array index is not an integer: " + indexValue.toString());
                }

                int index = ((IntegerValue) indexValue).getValue();


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


            Value val = null;
            if (node.getInitializer() != null) {
                val = node.getInitializer().apply(this, question);
            } else {

                if (node.getSize() == null || node.getSize().isEmpty()) {
                    throw new InterpreterException("Array size cannot be unspecified when no initializer is specified: " + node.getName());
                }

                //array deceleration
                IntegerValue size = (IntegerValue) node.getSize().get(0).apply(this, question);
                val = new ArrayValue<>(IntStream.range(0, size.getValue()).mapToObj(i -> new UndefinedValue()).collect(Collectors.toList()));
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
        return super.caseAArrayInitializer(node, question);
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
    public Value caseACallExp(ACallExp node, Context question) throws AnalysisException {

        Value function = node.getRoot().apply(this, question);

        if (function instanceof FunctionValue) {

            return ((FunctionValue) function).evaluate(evaluate(node.getArgs(), question));
        }

        throw new InterpreterException("Unhandled node: " + node);
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
    public Value createNewReturnValue(INode node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter node: {}", node.getClass().getSimpleName());
        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value createNewReturnValue(Object node, Context question) throws AnalysisException {
        throw new InterpreterException("Unhandled node: " + node);
    }
}
