package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
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
    private final IExternalValueFactory loadFactory;

    public Interpreter(IExternalValueFactory loadFactory) {
        this.loadFactory = loadFactory;
    }

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

        Context ctxt = new Context(question);
        for (PStm stm : node.getBody()) {
            stm.apply(this, ctxt);
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
        if (this.loadFactory.supports(type)) {
            Either<Exception, Value> valueE = this.loadFactory.create(type, args.subList(1, args.size()));
            if (valueE.isLeft()) {
                throw new AnalysisException(valueE.getLeft());
            } else {
                return valueE.getRight();
            }
        }
        throw new AnalysisException("Load of unknown type");
    }

    @Override
    public Value caseAUnloadExp(AUnloadExp node, Context question) throws AnalysisException {

        List<Value> args = evaluate(node.getArgs(), question).stream().map(Value::deref).collect(Collectors.toList());

        Value nameVal = args.get(0);
        return this.loadFactory.destroy(nameVal);
    }


    @Override
    public Value caseAIdentifierStateDesignator(AIdentifierStateDesignator node, Context question) throws AnalysisException {
        return question.lookup(node.getName());
    }

    @Override
    public Value caseAArrayStateDesignator(AArrayStateDesignator node, Context question) throws AnalysisException {

        Value arrayValue = node.getTarget().apply(this, question);

        if (!(arrayValue.deref() instanceof ArrayValue)) {
            throw new InterpreterException("Array designator is not an array: " + arrayValue);
        }

        return arrayValue;
    }

    @Override
    public Value caseAAssigmentStm(AAssigmentStm node, Context question) throws AnalysisException {

        Value newValue = node.getExp().apply(this, question);


        Value currentValue = node.getTarget().apply(this, question);
        if (!(currentValue instanceof UpdatableValue)) {
            throw new InterpreterException("Cannot assign to a constant value");
        }

        UpdatableValue currentUpdatableValue = (UpdatableValue) currentValue;


        if (currentUpdatableValue.deref() instanceof ArrayValue) {

            if (node.getTarget() instanceof AArrayStateDesignator) {
                AArrayStateDesignator arrayStateDesignator = (AArrayStateDesignator) node.getTarget();

                if (arrayStateDesignator.getExp() == null) {
                    //replace array completly
                    currentUpdatableValue.setValue(newValue);
                } else {
                    //in-place array update
                    Value indexValue = arrayStateDesignator.getExp().apply(this, question);

                    if (!(indexValue instanceof NumericValue)) {
                        throw new InterpreterException("Array index is not an integer: " + indexValue.toString());
                    }

                    int index = ((NumericValue) indexValue).intValue();
                    ArrayValue<Value> arrayValue = (ArrayValue<Value>) currentUpdatableValue.deref();
                    if (index >= 0 && index < arrayValue.getValues().size()) {
                        arrayValue.getValues().set(index, newValue);
                    } else {
                        throw new InterpreterException("Array index out of bounds: " + indexValue.toString());
                    }
                }

            } else {
                throw new InterpreterException("Bad array designator: " + node.getTarget().toString());
            }
        } else {
            currentUpdatableValue.setValue(newValue);
        }


        return new VoidValue();

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

            question.put(node.getName(), new UpdatableValue(val));

        } else {

            question.put(node.getName(),
                    new UpdatableValue(node.getInitializer() == null ? new UndefinedValue() : node.getInitializer().apply(this, question)));
        }
        return new VoidValue();
    }

    @Override
    public Value caseAExpInitializer(AExpInitializer node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value caseAArrayInitializer(AArrayInitializer node, Context question) throws AnalysisException {
        ArrayValue<Value> array = new ArrayValue<>(evaluate(node.getExp(), question).stream().map(Value::deref).collect(Collectors.toList()));
        return array;
    }

    @Override
    public Value caseAFieldExp(AFieldExp node, Context question) throws AnalysisException {
        Value root = node.getRoot().apply(this, question);

        if (root instanceof ModuleValue) {

            ModuleContext moduleContext = new ModuleContext((ModuleValue) root, question);
            return moduleContext.lookup(node.getField());

        }
        throw new InterpreterException("Unhandled node: " + node);

    }


    @Override
    public Value caseAExpressionStm(AExpressionStm node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }


    @Override
    public Value caseAIdentifierExp(AIdentifierExp node, Context question) throws AnalysisException {
        Value val = question.lookup(node.getName());
        if (val == null) {
            throw new InterpreterException("Variable undefined: '" + node.getName() + "':" + node.getName().getSymbol().getLine());
        }
        return val;
    }

    @Override
    public Value caseAPlusBinaryExp(APlusBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();


        if (left instanceof IntegerValue && right instanceof IntegerValue) {
            return new IntegerValue(left.intValue() + right.intValue());
        }

        return new RealValue(left.realValue() + right.realValue());
    }


    @Override
    public Value caseAMinusBinaryExp(AMinusBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();


        if (left instanceof IntegerValue && right instanceof IntegerValue) {
            return new IntegerValue(left.intValue() + right.intValue());
        }

        return new RealValue(left.realValue() - right.realValue());
    }

    @Override
    public Value caseACallExp(ACallExp node, final Context question) throws AnalysisException {

        Context callContext = question;
        if (node.getObject() != null) {
            ModuleValue objectModule = (ModuleValue) node.getObject().apply(this, question).deref();
            callContext = new ModuleContext(objectModule, question);
        }

        Value function = callContext.lookup(node.getMethodName());

        if (function instanceof FunctionValue) {

            return ((FunctionValue) function).evaluate(evaluate(node.getArgs(), callContext));
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
        if (((BooleanValue) node.getTest().apply(this, question).deref()).getValue()) {
            node.getThen().apply(this, question);
        } else if (node.getElse() != null) {
            node.getElse().apply(this, question);
        }
        return new VoidValue();
    }

    @Override
    public Value caseALessBinaryExp(ALessBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();

        return new BooleanValue(left.deref().compareTo(right.deref()) < 0);
    }

    @Override
    public Value caseALessEqualBinaryExp(ALessEqualBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();

        return new BooleanValue(left.deref().compareTo(right.deref()) <= 0);
    }

    @Override
    public Value caseAGreaterBinaryExp(AGreaterBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();

        return new BooleanValue(left.deref().compareTo(right.deref()) > 0);
    }

    @Override
    public Value caseAEqualBinaryExp(AEqualBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();

        return new BooleanValue(left.deref().compareTo(right.deref()) == 0);
    }

    @Override
    public Value caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();

        return new BooleanValue(left.deref().compareTo(right.deref()) >= 0);
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
