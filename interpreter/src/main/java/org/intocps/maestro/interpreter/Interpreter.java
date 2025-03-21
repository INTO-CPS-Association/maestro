package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.utilities.ArrayUpdatableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Interpreter extends QuestionAnswerAdaptor<Context, Value> {

    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);
    static final Map<Class<? extends PType>, Class<? extends Value>> typeValueMappings = new HashMap<>() {{
        put(AIntNumericPrimitiveType.class, IntegerValue.class);
        put(AByteNumericPrimitiveType.class, ByteValue.class);
        put(AShortNumericPrimitiveType.class, ShortValue.class);
        put(AUIntNumericPrimitiveType.class, UnsignedIntegerValue.class);
        put(ALongNumericPrimitiveType.class, LongValue.class);
        put(AFloatNumericPrimitiveType.class, FloatValue.class);
        put(ARealNumericPrimitiveType.class, RealValue.class);
        put(AStringPrimitiveType.class, StringValue.class);
        put(ABooleanPrimitiveType.class, BooleanValue.class);
        put(ANameType.class, ExternalModuleValue.class);
    }};
    private final IExternalValueFactory loadFactory;
    private final ITransitionManager transitionManager;

    public Interpreter(IExternalValueFactory loadFactory) {
        this(loadFactory, null);
    }

    public Interpreter(IExternalValueFactory loadFactory, ITransitionManager transitionManager) {
        this.loadFactory = loadFactory;
        this.transitionManager = transitionManager;
    }

    public IExternalValueFactory getLoadFactory() {
        return loadFactory;
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
    public Value caseABasicBlockStm(ABasicBlockStm node, Context question) throws AnalysisException {

        Context ctxt = new Context(question);
        for (PStm stm : node.getBody()) {
            stm.apply(this, ctxt);
        }
        return new VoidValue();
    }

    @Override
    public Value caseAParallelBlockStm(AParallelBlockStm node, org.intocps.maestro.interpreter.Context question) throws AnalysisException {
        Context ctxt = new Context(question);
        Optional<Optional<?>> errors = node.getBody().stream().parallel().map(s -> {
            try {
                s.apply(this, ctxt);
            } catch (AnalysisException e) {
                return Optional.of(e);
            }
            return Optional.empty();
        }).findFirst();

        if (errors.isPresent() && errors.get().isPresent()) {
            throw (AnalysisException) errors.get().get();
        }
        return new VoidValue();
    }

    @Override
    public Value caseAInstanceMappingStm(AInstanceMappingStm node, Context question) {
        return new VoidValue();
    }

    @Override
    public Value caseAFmuMappingStm(AFmuMappingStm node, Context question) throws AnalysisException {
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
            ArrayValue<Value> array = (ArrayValue<Value>) arrayValue.deref();
            int index = ((NumericValue) node.getExp().apply(this, question).deref()).intValue();
            return new ArrayUpdatableValue(array, index);
        }
    }

    @Override
    public Value caseAAssigmentStm(AAssigmentStm node, Context question) throws AnalysisException {

        Value newValue = node.getExp().apply(this, question);

        Value currentValue = node.getTarget().apply(this, question);
        if (!(currentValue instanceof UpdatableValue)) {
            throw new InterpreterException("Cannot assign to a constant value. " + node);
        }

        try {
            UpdatableValue currentUpdatableValue = (UpdatableValue) currentValue;
            currentUpdatableValue.setValue(newValue.deref());
        } catch (Exception e) {
            throw new RuntimeException("Failed at: " + node, e);
        }
        return new VoidValue();
    }

    @Override
    public Value caseANotEqualBinaryExp(ANotEqualBinaryExp node, Context question) throws AnalysisException {
        BooleanValue equals = equals(node.getLeft().apply(this, question), node.getRight().apply(this, question));
        return new BooleanValue(!equals.getValue());
    }


    public ArrayValue createArrayValue(List<PExp> sizes, PType type, Context question) throws AnalysisException {
        List<Value> arrayValues = new ArrayList<>();
        for (int i = 0; i < ((NumericValue) sizes.get(0).apply(this, question).deref()).intValue(); i++) {
            if (sizes.size() > 1) {
                List<PExp> nextSizes = sizes.subList(1, sizes.size());
                // Call recursively
                arrayValues.add(new UpdatableValue(createArrayValue(nextSizes, type, question)));
            } else {

                if (type instanceof AFunctionType || type instanceof ARealNumericPrimitiveType) {
                    arrayValues.add(new RealValue(0.0));
                } else if (type instanceof SNumericPrimitiveType) {
                    arrayValues.add(new ByteValue(0));
                } else if (type instanceof ABooleanPrimitiveType) {
                    arrayValues.add(new BooleanValue(false));
                } else if (type instanceof AStringPrimitiveType) {
                    arrayValues.add(new StringValue(""));
                } else {
                    arrayValues.add(new NullValue());
                }
            }
        }
        return new ArrayValue<>(arrayValues);
    }

    @Override
    public Value caseAVariableDeclaration(AVariableDeclaration node, Context question) throws AnalysisException {

        if (node.getExternal() != null && node.getExternal()) {
            //external variables already exists in the context
            return new VoidValue();
        }

        if (!node.getSize().isEmpty() /*lazy check for array type*/) {

            ArrayValue arrayValue;
            if (node.getInitializer() != null) {
                arrayValue = (ArrayValue) node.getInitializer().apply(this, question);
            } else {

                if (node.getSize() == null || node.getSize().isEmpty()) {
                    throw new InterpreterException("Array size cannot be unspecified when no .conlizer is specified: " + node.getName());
                }

                //array deceleration
                arrayValue = createArrayValue(node.getSize(), node.getType(), question);
            }

            //DTC: check that all values are of the right type
            Class<? extends Value> targetValueType = typeValueMappings.get(node.getType().getClass());
            for (int i = 0; i < arrayValue.getValues().size(); i++) {

                Value v = (Value) arrayValue.getValues().get(i);
                if (targetValueType == null || targetValueType.isAssignableFrom(v.getClass())) {
                    continue;
                } else if (v.isNumeric()) {
                    //we need to upcast all values explicitly here
                    NumericValue upcasted = ((NumericValue) v).upCast((Class<? extends NumericValue>) targetValueType);

                    if (upcasted == null) {
                        throw new InterpreterException(
                                String.format("Array initializer value at index %d '%s' could not be upcasted. In ", i, v.toString()) +
                                        "initializer is " + "specified: " + node.getName());
                    }

                    arrayValue.getValues().set(i, upcasted);
                }


            }
            //            if (arrayValue.getValues().stream().anyMatch(v -> !targetValueType.isAssignableFrom(v.getClass()))) {
            //
            //                arrayValue.getValues().stream().filter(v -> !targetValueType.isAssignableFrom(v.getClass())).map(v -> v.toString())
            //                        .collect(Collectors.joining(","));
            //            }

            question.put(node.getName(), new UpdatableValue(arrayValue));

        } else {

            Value initialValue = node.getInitializer() == null ? new UndefinedValue() : node.getInitializer().apply(this, question);

            if (initialValue.deref().isNumeric()) {
                Class<? extends Value> targetValueType = typeValueMappings.get(node.getType().getClass());

                if (targetValueType != null) {

                    //we need to upcast all values explicitly here
                    NumericValue upcasted = ((NumericValue) initialValue.deref()).upCast((Class<? extends NumericValue>) targetValueType);

                    if (upcasted == null) {
                        throw new InterpreterException(
                                String.format("Initializer value could not be upcasted. In ", initialValue.deref().toString()) + "initializer is " +
                                        "specified: " + node.getName() + " in " + node);
                    }
                    initialValue = upcasted;
                }
            }


            question.put(node.getName(), new UpdatableValue(initialValue));
        }
        return new VoidValue();
    }

    @Override
    public Value caseAExpInitializer(AExpInitializer node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value caseAArrayInitializer(AArrayInitializer node, Context question) throws AnalysisException {
        ArrayValue<Value> array = new ArrayValue<>(evaluate(node.getExp(), question).stream().map(v -> v.deref()).collect(Collectors.toList()));
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


        if (!left.isNumericDecimal() && !right.isNumericDecimal()) {
            return new IntegerValue(left.intValue() + right.intValue());
        } else {
            return new RealValue(left.realValue() + right.realValue());
        }
    }


    @Override
    public Value caseAMinusBinaryExp(AMinusBinaryExp node, Context question) throws AnalysisException {
        NumericValue left = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue right = (NumericValue) node.getRight().apply(this, question).deref();


        if (!left.isNumericDecimal() && !right.isNumericDecimal()) {
            return new IntegerValue(left.intValue() - right.intValue());
        }

        return new RealValue(left.realValue() - right.realValue());
    }

    @Override
    public Value caseACallExp(ACallExp node, final Context question) throws AnalysisException {

        Context callContext = question;
        if (node.getObject() != null) {
            Value v = node.getObject().apply(this, question).deref();
            if (v instanceof NullValue) {
                logger.error("The target object: \"" + node.getObject().toString() + "\" is null. Related call: \"" + node.toString() + "\"");
                throw new InterpreterException("Unhandled node: " + node);
            } else {
                ModuleValue objectModule = (ModuleValue) v;
                callContext = new ModuleContext(objectModule, question);
            }
        }

        Value function = callContext.lookup(node.getMethodName());

        if (function instanceof FunctionValue) {
            try {
                return ((FunctionValue) function).evaluate(evaluate(node.getArgs(), callContext));
            } catch (InterpreterTransitionException te) {
                throw te;
            } catch (Exception e) {
                throw new InterpreterException("Unable to evaluate node: " + node, e);
            }
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
    public Value caseATryStm(ATryStm node, Context question) throws AnalysisException {
        try {
            node.getBody().apply(this, question);
        } catch (ErrorException e) {
            logger.info("Error in simulation: " + e.getMessage());
            logger.info("Continuing with finally");
            node.getFinally().apply(this, question);
            throw e;
        } catch (StopException e) {
            logger.info("Stop in simulation: " + e.getMessage());
            logger.info("Continuing with finally");
        }
        node.getFinally().apply(this, question);
        return new VoidValue();
    }

    @Override
    public Value caseAErrorStm(AErrorStm node, Context question) throws AnalysisException {

        String message = "";
        if (node.getExp() != null) {
            Value msg = node.getExp().apply(this, question).deref();
            if (msg instanceof StringValue) {
                message = ((StringValue) msg).getValue();
            } else {
                message = msg.toString();
            }
        }

        throw new ErrorException(message);
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
            return new BooleanValue(true);
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
        return NumericValue.valueOf(node.getValue());
    }

    @Override
    public Value caseAFloatLiteralExp(AFloatLiteralExp node, Context question) throws AnalysisException {
        return NumericValue.valueOf(node.getValue());
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
        return NumericValue.valueOf(node.getValue());
    }

    @Override
    public Value caseAUIntLiteralExp(AUIntLiteralExp node, Context question) throws AnalysisException {
        return NumericValue.valueOf(node.getValue());
    }

    protected Value getInnerArrayValue(ArrayValue<Value> arrayValue, List<NumericValue> indices) {
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
    public Value caseAMultiplyBinaryExp(AMultiplyBinaryExp node, Context question) throws AnalysisException {

        NumericValue x = (NumericValue) node.getLeft().apply(this, question).deref();
        NumericValue y = (NumericValue) node.getRight().apply(this, question).deref();

        if (x instanceof RealValue || y instanceof RealValue) {
            return new RealValue(x.realValue() * y.realValue());
        }

        return new IntegerValue(x.intValue() * y.intValue());
    }

    @Override
    public Value caseATransferStm(ATransferStm node, Context question) throws AnalysisException {
        if (transitionManager != null) {

            ITransitionManager.ITTransitionInfo info = transitionManager.getTransferInfo(node, question,
                    node.getNames().stream().limit(1).map(AStringLiteralExp::getValue).findFirst().orElse(null));
            if (info != null) {

                transitionManager.transfer(this, info);
                //if we get back here we need to break out of the current state and run the shutdown actions from finally
                throw new StopException("Stopping previous simulation as a result of a model transfer: " + info.describe());
            }
        }
        return new VoidValue();
    }

    @Override
    public Value caseATransferAsStm(ATransferAsStm node, Context question) {
        //this has no semantic meaning during normal execution
        return new VoidValue();
    }

    @Override
    public Value createNewReturnValue(INode node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter node: {}", node.getClass().getSimpleName());
        throw new InterpreterException("Unhandled node: " + node);
    }

    @Override
    public Value caseARefExp(ARefExp node, Context question) throws AnalysisException {
        ByRefInterpreter byRefInterpreter = new ByRefInterpreter(this.loadFactory);
        return node.getExp().apply(byRefInterpreter, question);
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