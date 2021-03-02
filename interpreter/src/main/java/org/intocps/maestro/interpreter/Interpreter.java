package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.interpreter.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

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
        AnalysisException[] exception = {null};
        Value result = node.getContent().stream().filter(f -> f instanceof ASimulationSpecificationCompilationUnit).findFirst().map(spec -> {
            try {
                return spec.apply(this, question);
            } catch (AnalysisException e) {
                exception[0] = e;
                return null;
            }
        }).orElse(null);

        if (result == null) {
            throw exception[0];
        }
        return result;
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
    public Value caseAInstanceMappingStm(AInstanceMappingStm node, Context question) {
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

        String loaderName = ((StringValue) args.get(0)).getValue();
        try {
            if (this.loadFactory.supports(loaderName)) {
                Either<Exception, Value> valueE = this.loadFactory.create(loaderName, args.subList(1, args.size()));
                if (valueE.isLeft()) {
                    throw new AnalysisException(valueE.getLeft());
                } else {
                    return valueE.getRight();
                }
            }
        } catch (Exception e) {
            throw new AnalysisException("Load failed", e);
        }
        throw new AnalysisException("Load of unknown type: " + loaderName);
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
        } else {
            ArrayValue array = (ArrayValue) arrayValue.deref();
            int index = ((NumericValue) node.getExp().apply(this, question).deref()).intValue();
            Value value = (Value) array.getValues().get(index);
            if (!(value instanceof UpdatableValue)) {
                return new UpdatableValue(value);
            } else {
                return value;
            }
        }
    }

    @Override
    public Value caseAAssigmentStm(AAssigmentStm node, Context question) throws AnalysisException {

        Value newValue = node.getExp().apply(this, question);

        Value currentValue = node.getTarget().apply(this, question);
        if (!(currentValue instanceof UpdatableValue)) {
            throw new InterpreterException("Cannot assign to a constant value");
        }

        UpdatableValue currentUpdatableValue = (UpdatableValue) currentValue;
        currentUpdatableValue.setValue(newValue.deref());

        return new VoidValue();
    }

    @Override
    public Value caseANotEqualBinaryExp(ANotEqualBinaryExp node, Context question) throws AnalysisException {
        BooleanValue equals = equals(node.getLeft().apply(this, question), node.getRight().apply(this, question));
        return new BooleanValue(!equals.getValue());
    }


    public UpdatableValue createArrayValue(List<PExp> sizes, PType type, Context question) throws AnalysisException {
        List<Value> arrayValues = new ArrayList<>();
        for (int i = 0; i < ((IntegerValue) sizes.get(0).apply(this, question)).getValue(); i++) {
            if (sizes.size() > 1) {
                List<PExp> nextSizes = sizes.subList(1, sizes.size());
                // Call recursively
                arrayValues.add(createArrayValue(nextSizes, type, question));
            } else {
                if (type instanceof AIntNumericPrimitiveType) {
                    arrayValues.add(new UpdatableValue(new IntegerValue(0)));
                } else if (type instanceof ABooleanPrimitiveType) {
                    arrayValues.add(new UpdatableValue(new BooleanValue(false)));
                } else if (type instanceof AStringPrimitiveType) {
                    arrayValues.add(new UpdatableValue(new StringValue("")));
                } else if (type instanceof ARealNumericPrimitiveType) {
                    arrayValues.add(new UpdatableValue(new RealValue(0.0)));
                } else {
                    arrayValues.add(new UpdatableValue(new NullValue()));
                }
            }
        }
        return new UpdatableValue(new ArrayValue<>(arrayValues));
    }

    @Override
    public Value caseAVariableDeclaration(AVariableDeclaration node, Context question) throws AnalysisException {


        if (!node.getSize().isEmpty() /*lazy check for array type*/) {

            UpdatableValue val;
            if (node.getInitializer() != null) {
                val = new UpdatableValue(node.getInitializer().apply(this, question));
            } else {

                if (node.getSize() == null || node.getSize().isEmpty()) {
                    throw new InterpreterException("Array size cannot be unspecified when no initializer is specified: " + node.getName());
                }

                //array deceleration
                val = createArrayValue(node.getSize(), node.getType(), question);
            }

            question.put(node.getName(), val);

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
    public Value caseAIdentifierExp(AIdentifierExp node, Context question) {
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
            return new IntegerValue(left.intValue() - right.intValue());
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

        try {
            while (((BooleanValue) (node.getTest().apply(this, question).deref())).getValue()) {
                node.getBody().apply(this, question);
            }
        } catch (BreakException e) {
            //loop stopped
            logger.trace("Loop stopped:" + node);
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

        return equals(node.getLeft().apply(this, question), node.getRight().apply(this, question));

        //        throw new InterpreterException(
        //                "Equality not implement for: " + lv.getClass().getSimpleName() + " == " + rv.getClass().getSimpleName() + " (" + lv + " == " + rv +
        //                        ")");
    }

    private BooleanValue equals(Value lv, Value rv) throws AnalysisException {
        lv = lv.deref();
        rv = rv.deref();

        if (lv.equals(rv)) {
            new BooleanValue(true);
        } else if (lv instanceof NumericValue && rv instanceof NumericValue) {
            NumericValue left = (NumericValue) lv;
            NumericValue right = (NumericValue) rv;

            return new BooleanValue(left.deref().compareTo(right.deref()) == 0);
        } else if (lv instanceof BooleanValue && rv instanceof BooleanValue) {
            BooleanValue left = (BooleanValue) lv;
            BooleanValue right = (BooleanValue) rv;

            return new BooleanValue(left.getValue().compareTo(right.getValue()) == 0);
        } else if (lv instanceof NullValue && rv instanceof NullValue) {
            return new BooleanValue(true);
        }

        return new BooleanValue(false);
    }

    @Override
    public Value caseAOrBinaryExp(AOrBinaryExp node, Context question) throws AnalysisException {
        return new BooleanValue(((BooleanValue) node.getLeft().apply(this, question).deref()).getValue() ||
                ((BooleanValue) node.getRight().apply(this, question).deref()).getValue());
    }

    @Override
    public Value caseAParExp(AParExp node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value caseAAndBinaryExp(AAndBinaryExp node, Context question) throws AnalysisException {
        return new BooleanValue(((BooleanValue) node.getLeft().apply(this, question).deref()).getValue() &&
                ((BooleanValue) node.getRight().apply(this, question).deref()).getValue());
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
        return new UnsignedIntegerValue(node.getValue());
    }

    private Value getInnerArrayValue(ArrayValue<Value> arrayValue, List<NumericValue> indices) {
        return (indices.size() > 1) ? getInnerArrayValue((ArrayValue<Value>) arrayValue.getValues().get(indices.get(0).intValue()).deref(),
                indices.subList(1, indices.size())) : arrayValue.getValues().get(indices.get(0).intValue());
    }

    @Override
    public Value caseAArrayIndexExp(AArrayIndexExp node, Context question) throws AnalysisException {
        Value value = node.getArray().apply(this, question).deref();

        if (value instanceof ArrayValue) {

            List<NumericValue> indices =
                    evaluate(node.getIndices(), question).stream().map(Value::deref).map(NumericValue.class::cast).collect(Collectors.toList());

            return getInnerArrayValue((ArrayValue) value, indices);
        }
        throw new AnalysisException("No array or index for: " + node);
    }

    @Override
    public Value caseANotUnaryExp(ANotUnaryExp node, Context question) throws AnalysisException {

        Value value = node.getExp().apply(this, question);

        if (!(value.deref() instanceof BooleanValue)) {
            throw new InterpreterException("Invalid type in not expression");
        }

        return new BooleanValue(!((BooleanValue) value.deref()).getValue());
    }

    @Override
    public Value caseABreakStm(ABreakStm node, Context question) throws AnalysisException {
        throw new BreakException();
    }

    @Override
    public Value caseANullExp(ANullExp node, Context question) throws AnalysisException {
        return new NullValue();
    }

    @Override
    public Value createNewReturnValue(INode node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter node: {}", node.getClass().getSimpleName());
        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value caseARefExp(ARefExp node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value createNewReturnValue(Object node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter object: {}", node.getClass().getSimpleName());
        throw new InterpreterException("Unhandled object: " + node);
    }

    @Override
    public Value caseAMinusUnaryExp(AMinusUnaryExp node, Context question) throws AnalysisException {
        NumericValue exp = (NumericValue) node.getExp().apply(this, question).deref();
        if (exp instanceof IntegerValue) {
            return new IntegerValue(exp.intValue() * (-1));
        } else {
            return new RealValue(exp.realValue() * (-1));
        }
    }
}
