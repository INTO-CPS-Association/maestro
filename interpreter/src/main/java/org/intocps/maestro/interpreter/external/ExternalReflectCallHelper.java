package org.intocps.maestro.interpreter.external;

import org.apache.commons.lang3.ArrayUtils;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.interpreter.values.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.interpreter.Fmi2Interpreter.checkArgLength;
import static org.intocps.maestro.interpreter.Fmi2Interpreter.getArrayValue;

public class ExternalReflectCallHelper extends Vector<IArgMapping> {


    protected final String functionName;
    protected final Object object;
    protected IArgMapping returnArg = null;

    public ExternalReflectCallHelper(String functionName, Object object) {
        this.functionName = functionName;
        this.object = object;
    }

    public ExternalReflectCallHelper(AFunctionDeclaration functionDeclaration, Object object,
            Function<ArgMappingContext, IArgMapping> costumeArgMapper) {
        this(functionDeclaration.getName().getText(), object);
        autoBuild(functionDeclaration, costumeArgMapper);
    }

    public ExternalReflectCallHelper(AFunctionDeclaration functionDeclaration, Object object) {
        this(functionDeclaration, object, null);
    }

    public static Map.Entry<TP, Integer> getReverseType(PType t) throws ExceptionUnknownTypeMapping {

        if (t instanceof AArrayType) {
            AArrayType at = ((AArrayType) t);
            return Map.entry(getReverseType(at.getType()).getKey(), Integer.valueOf(2));
        }

        if (t instanceof AReferenceType) {
            return getReverseType(((AReferenceType) t).getType());
        }

        Map<Class<? extends PType>, TP> mapping = new HashMap<>() {{
            put(ABooleanPrimitiveType.class, TP.Bool);
            put(AByteNumericPrimitiveType.class, TP.Byte);
            put(AShortNumericPrimitiveType.class, TP.Short);
            put(AIntNumericPrimitiveType.class, TP.Int);
            put(AUIntNumericPrimitiveType.class, TP.Long);
            put(AFloatNumericPrimitiveType.class, TP.Float);
            put(ARealNumericPrimitiveType.class, TP.Real);
            put(ALongNumericPrimitiveType.class, TP.Long);
            put(AStringPrimitiveType.class, TP.String);
        }};

        var tp = mapping.get(t.getClass());
        if (tp == null) {
            throw new ExceptionUnknownTypeMapping("Type mapping unknown for the type: " + t);
        }
        return Map.entry(tp, 1);


    }

    public String getSignature(boolean body) {
        StringBuilder sb = new StringBuilder();
        sb.append("public ");
        if (returnArg != null) {
            sb.append(returnArg.getType().getSimpleName());
        } else {
            sb.append("void");
        }
        sb.append(" ");
        sb.append(functionName);
        sb.append("(");
        //            sb.append(stream().map(a -> a.getType().getSimpleName()).collect(Collectors.joining(", ")));
        for (int i = 0; i < size(); i++) {
            sb.append(get(i).getType().getSimpleName());
            sb.append(" arg" + i);
            if (i + 1 < size()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        if (body) {
            sb.append("{");
            for (int i = 0; i < size(); i++) {
                IArgMapping argMapping = get(i);
                if (argMapping.getDirection() == ArgMapping.InOut.Output) {
                    sb.append("arg" + i + "[0] = ");
                    sb.append(returnArg.getDefaultTestValue());
                    sb.append(";");
                }
            }
            if (returnArg != null) {

                sb.append("return " + returnArg.getDefaultTestValue() + ";");

                sb.append("}");
            }
        } else {
            sb.append(";");
        }
        return sb.toString();
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getSignature(false) + "\n");


        sb.append(functionName);
        sb.append("\n");
        for (int i = 0; i < size(); i++) {
            IArgMapping argMapping = get(i);
            sb.append(String.format("\t%2d: ", i));
            sb.append(String.format("%-6s", argMapping.getDirection()));
            sb.append(" ");
            sb.append(String.format("%-8s", argMapping.getDescriptiveName() + IntStream.range(1, argMapping.getDimension()).mapToObj(n -> "[]")
                    .collect(Collectors.joining())));
            sb.append(" -- ");
            sb.append(argMapping.getType().getSimpleName());
            sb.append("\n");

        }

        if (returnArg != null) {
            sb.append("\t *: ");
            sb.append(String.format("%-6s", returnArg.getDirection()));
            sb.append(" ");
            sb.append(String.format("%-8s",
                    returnArg.getDescriptiveName() + IntStream.range(1, returnArg.getDimension()).mapToObj(n -> "[]").collect(Collectors.joining())));
            sb.append(" -- ");
            sb.append(returnArg.getType().getSimpleName());
            sb.append("\n");
        }

        return sb.toString();
    }

    public static class ArgMappingContext {
        final String functionName;
        final String argName;

        public String getFunctionName() {
            return functionName;
        }

        public String getArgName() {
            return argName;
        }

        public PType getArgType() {
            return argType;
        }

        final PType argType;

        public ArgMappingContext(String functionName, String argName, PType argType) {
            this.functionName = functionName;
            this.argName = argName;
            this.argType = argType;
        }
    }

    private void autoBuild(AFunctionDeclaration functionDeclaration, Function<ArgMappingContext, IArgMapping> costumeArgMapper) {
        PType returnType = functionDeclaration.getReturnType();
        if (returnType != null && !(returnType instanceof AVoidType)) {
            try {
                IArgMapping ca;
                if (costumeArgMapper != null && (ca = costumeArgMapper.apply(new ArgMappingContext(this.functionName, null, returnType))) != null) {
                    addReturn(ca);
                } else {
                    addReturn(getReverseType(returnType).getKey());
                }
            } catch (ExceptionUnknownTypeMapping e) {
                //                IArgMapping ca;
                //                if (costumeArgMapper != null && (ca = costumeArgMapper.apply(new ArgMappingContext(this.functionName, null, returnType))) != null) {
                //                    addReturn(ca);
                //                } else {
                throw new RuntimeException(e);
                //                }
            }
        }

        for (AFormalParameter formal : functionDeclaration.getFormals()) {
            try {
                var tDim = getReverseType(formal.getType());
                addArg(tDim.getKey(), tDim.getValue(), formal.getType() instanceof AReferenceType ? ArgMapping.InOut.Output : ArgMapping.InOut.Input);
            } catch (ExceptionUnknownTypeMapping e) {
                IArgMapping ca;
                if (costumeArgMapper != null && (ca = costumeArgMapper.apply(
                        new ArgMappingContext(this.functionName, formal.getName().getText(), formal.getType()))) != null) {
                    add(ca);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public ExternalReflectCallHelper addReturn(TP type) {
        return addReturn(new ArgMapping(type, 1, ArgMapping.InOut.Output, null));
    }

    public ExternalReflectCallHelper addReturn(IArgMapping returnArg) {
        this.returnArg = returnArg;
        return this;
    }

    public ExternalReflectCallHelper addArg(TP type) {
        this.add(new ArgMapping(type));
        return this;
    }

    public ExternalReflectCallHelper addArg(TP type, int dimiention) {
        this.add(new ArgMapping(type, dimiention, ArgMapping.InOut.Input, null));
        return this;
    }

    public ExternalReflectCallHelper addArg(TP type, int dimiention, ArgMapping.InOut direction) {
        this.add(new ArgMapping(type, dimiention, direction, null));
        return this;
    }

    public ExternalReflectCallHelper addArg(TP type, int dimiention, ArgMapping.InOut direction, long... limits) {
        this.add(new ArgMapping(type, dimiention, direction, limits));
        return this;
    }

    public FunctionValue.ExternalFunctionValue build() throws NoSuchMethodException {

        final List<IArgMapping> args = Collections.unmodifiableList(this);
        final IArgMapping rArg = returnArg;
        final Method method = object.getClass().getMethod(functionName,
                args.stream().filter(arg -> arg.getDirection() != ArgMapping.InOut.OutputThroughReturn).map(IArgMapping::getType)
                        .toArray(Class[]::new));


        return new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, args.size());

            //map inputs
            var passedArgItr = fcargs.iterator();
            var declaredArgItr = args.iterator();
            List argValues = new ArrayList(args.size());
            while (passedArgItr.hasNext() && declaredArgItr.hasNext()) {
                var v = passedArgItr.next();
                IArgMapping mapper = declaredArgItr.next();
                if (mapper.getDirection() != ArgMapping.InOut.OutputThroughReturn) {
                    argValues.add(mapper.map(v));
                }
            }

            try {
                var ret = method.invoke(object, argValues.toArray());

                //map outputs
                passedArgItr = fcargs.iterator();
                var passedValuesItr = argValues.iterator();
                declaredArgItr = args.iterator();
                Map<IArgMapping, Value> outputThroughReturn = new HashMap<>();

                // process arguments. All inputs should be skipped and output mapped out. All out through return should be collected and handled
                // through the mapout of return as additional info
                while (passedArgItr.hasNext() && declaredArgItr.hasNext()) {
                    IArgMapping mapper = declaredArgItr.next();
                    var original = passedArgItr.next();

                    if (mapper.getDirection() == ArgMapping.InOut.OutputThroughReturn) {
                        outputThroughReturn.put(mapper, original);
                        continue;
                    }
                    if (passedValuesItr.hasNext()) {
                        var passedValue = passedValuesItr.next();
                        if (mapper.getDirection() == ArgMapping.InOut.Output) {
                            mapper.mapOut(original, passedValue);
                        }
                    }
                }


                if (rArg == null) {
                    return new VoidValue();
                }
                return rArg.mapOut(ret, outputThroughReturn);


            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static class ExceptionUnknownTypeMapping extends Exception {
        public ExceptionUnknownTypeMapping(String message) {
            super(message);
        }
    }

    public static class ArgMapping implements IArgMapping {

        final TP type;
        final int dimension;
        final long[] limits;

        InOut direction;

        public ArgMapping(TP type, int dimension, InOut direction, long[] limits) {
            this.type = type;
            this.dimension = dimension;
            this.direction = direction;
            this.limits = limits;
        }

        public ArgMapping(TP type) {
            this(type, 1, InOut.Input, null);
        }

        @Override
        public int getDimension() {
            return dimension;
        }

        @Override
        public long[] getLimits() {
            return limits;
        }

        @Override
        public InOut getDirection() {
            return direction;
        }

        @Override
        public void setDirection(InOut direction) {
            this.direction = direction;
        }

        @Override
        public Object map(Value v) {
            //map value to primitive type
            if (direction == InOut.Input) {
                v = v.deref();
            }

            if (dimension == 1) {
                switch (type) {

                    case Bool:
                        return ((BooleanValue) v).getValue();
                    case Byte: {
                        var n = ((NumericValue) v);
                        return Integer.valueOf(n.intValue()).byteValue();
                    }
                    case Float: {
                        var n = ((NumericValue) v);
                        return n.floatValue();//Integer.valueOf(n.intValue()).floatValue();
                    }
                    case Int: {
                        var n = ((NumericValue) v);
                        return n.intValue();
                    }
                    case Long: {
                        var n = ((NumericValue) v);
                        return Integer.valueOf(n.intValue()).longValue();
                    }
                    case Real: {
                        var n = ((NumericValue) v);
                        return Integer.valueOf(n.intValue()).doubleValue();
                    }
                    case Short: {
                        var n = ((NumericValue) v);
                        return Integer.valueOf(n.intValue()).shortValue();
                    }
                    case String:
                        return ((StringValue) v).getValue();
                }
            } else if (dimension == 2) {
                Optional<Long> limit = limits == null ? Optional.empty() : Optional.of(limits[0]);
                switch (type) {

                    case Bool:
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, BooleanValue.class).stream().map(BooleanValue::getValue).collect(Collectors.toList())
                                        .toArray(new Boolean[]{}));
                    case Byte:
                        //todo change native type to use int as byte us unsigned
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, NumericValue.class).stream().map(NumericValue::intValue).map(vb -> vb & 0x00ff)
                                        .map(Integer::byteValue).collect(Collectors.toList()).toArray(new Byte[]{}));
                    case Float:
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, NumericValue.class).stream().map(NumericValue::floatValue).collect(Collectors.toList())
                                        .toArray(new Float[]{}));
                    case Int:
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, NumericValue.class).stream().map(NumericValue::intValue).collect(Collectors.toList())
                                        .toArray(new Integer[]{}));
                    case Long:
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, NumericValue.class).stream().map(NumericValue::longValue).collect(Collectors.toList())
                                        .toArray(new Long[]{}));
                    case Real:
                        return getArrayValue(v, limit, NumericValue.class).stream().mapToDouble(NumericValue::realValue).toArray();
                    case Short:
                        return ArrayUtils.toPrimitive(
                                getArrayValue(v, limit, NumericValue.class).stream().map(NumericValue::intValue).map(Integer::shortValue)
                                        .collect(Collectors.toList()).toArray(new Short[]{}));
                    case String:
                        return getArrayValue(v, limit, StringValue.class).stream().map(StringValue::getValue).collect(Collectors.toList())
                                .toArray(new String[]{});
                }
            }
            return null;
        }

        @Override
        public void mapOut(Value original, Object value) {
            //TODO write back the value into the original value
            UpdatableValue ref = (UpdatableValue) original;

            List<Value> values = null;
            if (dimension == 2) {
                long elementsToUse = limits == null ? Long.MAX_VALUE : limits[0];
                switch (type) {

                    case Bool:
                        values = Arrays.stream(ArrayUtils.toObject((boolean[]) (value.getClass().isArray() ? value : new boolean[]{(boolean) value})))
                                .limit(elementsToUse).map(BooleanValue::new).collect(Collectors.toList());
                        break;
                    case Byte:
                        values = Arrays.stream(ArrayUtils.toObject((byte[]) (value.getClass().isArray() ? value : new byte[]{(byte) value})))
                                .limit(elementsToUse).map(ByteValue::new).collect(Collectors.toList());
                        break;
                    case Float:
                        values = Arrays.stream(ArrayUtils.toObject((float[]) (value.getClass().isArray() ? value : new float[]{(float) value})))
                                .limit(elementsToUse).map(FloatValue::new).collect(Collectors.toList());
                        break;
                    case Int:
                        values = Arrays.stream(ArrayUtils.toObject((int[]) (value.getClass().isArray() ? value : new int[]{(int) value})))
                                .limit(elementsToUse).map(IntegerValue::new).collect(Collectors.toList());
                        break;
                    case Long:
                        values = Arrays.stream(ArrayUtils.toObject((long[]) (value.getClass().isArray() ? value : new long[]{(long) value})))
                                .limit(elementsToUse).map(LongValue::new).collect(Collectors.toList());
                        break;
                    case Real:
                        values = Arrays.stream(ArrayUtils.toObject((double[]) (value.getClass().isArray() ? value : new double[]{(double) value})))
                                .limit(elementsToUse).map(RealValue::new).collect(Collectors.toList());
                        break;
                    case Short:
                        values = Arrays.stream(ArrayUtils.toObject((short[]) (value.getClass().isArray() ? value : new short[]{(short) value})))
                                .limit(elementsToUse).map(ShortValue::new).collect(Collectors.toList());
                        break;
                    case String:
                        values = Arrays.stream((String[]) (value.getClass().isArray() ? value : new String[]{(String) value})).limit(elementsToUse)
                                .map(StringValue::new).collect(Collectors.toList());
                        break;
                }
                ref.setValue(new ArrayValue<>(values));
            }

        }

        @Override
        public Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn) {
            //todo create value from value


            if (dimension == 2) {
                List<Value> values = null;
                long elementsToUse = limits == null ? Long.MAX_VALUE : limits[0];
                switch (type) {

                    case Bool:
                        values = Arrays.stream(ArrayUtils.toObject((boolean[]) (value.getClass().isArray() ? value : new boolean[]{(boolean) value})))
                                .limit(elementsToUse).map(BooleanValue::new).collect(Collectors.toList());
                        break;
                    case Byte:
                        values = Arrays.stream(ArrayUtils.toObject((byte[]) (value.getClass().isArray() ? value : new byte[]{(byte) value})))
                                .limit(elementsToUse).map(ByteValue::new).collect(Collectors.toList());
                        break;
                    case Float:
                        values = Arrays.stream(ArrayUtils.toObject((float[]) (value.getClass().isArray() ? value : new float[]{(float) value})))
                                .limit(elementsToUse).map(FloatValue::new).collect(Collectors.toList());
                        break;
                    case Int:
                        values = Arrays.stream(ArrayUtils.toObject((int[]) (value.getClass().isArray() ? value : new int[]{(int) value})))
                                .limit(elementsToUse).map(IntegerValue::new).collect(Collectors.toList());
                        break;
                    case Long:
                        values = Arrays.stream(ArrayUtils.toObject((long[]) (value.getClass().isArray() ? value : new long[]{(long) value})))
                                .limit(elementsToUse).map(LongValue::new).collect(Collectors.toList());
                        break;
                    case Real:
                        values = Arrays.stream(ArrayUtils.toObject((double[]) (value.getClass().isArray() ? value : new double[]{(double) value})))
                                .limit(elementsToUse).map(RealValue::new).collect(Collectors.toList());
                        break;
                    case Short:
                        values = Arrays.stream(ArrayUtils.toObject((short[]) (value.getClass().isArray() ? value : new short[]{(short) value})))
                                .limit(elementsToUse).map(ShortValue::new).collect(Collectors.toList());
                        break;
                    case String:
                        values = Arrays.stream((String[]) (value.getClass().isArray() ? value : new String[]{(String) value})).limit(elementsToUse)
                                .map(StringValue::new).collect(Collectors.toList());
                        break;
                }
                return new ArrayValue<>(values);
            } else {


                switch (type) {

                    case Bool:
                        return new BooleanValue((Boolean) value);
                    case Byte:
                        return new ByteValue((Integer) value);
                    case Float:
                        return new FloatValue((Float) value);
                    case Int:
                        return new IntegerValue((Integer) value);
                    case Long:
                        return new LongValue((Long) value);
                    case Real:
                        return new RealValue((Double) value);
                    case Short:
                        return new ShortValue((Short) value);
                    case String:
                        return new StringValue((String) value);
                }
            }
            throw new IllegalArgumentException("No mapping for value:" + value);
        }

        @Override
        public Class getType() {
            //reflect search type
            if (dimension == 1) {
                switch (type) {

                    case Bool:
                        return boolean.class;
                    case Byte:
                        return byte.class;
                    case Float:
                        return float.class;
                    case Int:
                        return int.class;
                    case Long:
                        return long.class;
                    case Real:
                        return double.class;
                    case Short:
                        return short.class;
                    case String:
                        return String.class;
                }
            } else if (dimension == 2) {
                switch (type) {

                    case Bool:
                        return boolean[].class;
                    case Byte:
                        return byte[].class;
                    case Float:
                        return float[].class;
                    case Int:
                        return int[].class;
                    case Long:
                        return long[].class;
                    case Real:
                        return double[].class;
                    case Short:
                        return short[].class;
                    case String:
                        return String[].class;
                }
            }

            throw new IllegalArgumentException("dimension not supported: " + dimension + " for type " + type);
        }

        @Override
        public String getDescriptiveName() {
            return type.toString();
        }

        @Override
        public String getDefaultTestValue() {
            switch (type) {

                case Bool:
                    return ("true");
                case Byte:
                case Int:
                case Long:

                case Short:
                    return ("1");
                case Float:
                case Real:
                    return ("1.1");

                case String:
                    return ("hi");
            }
            return "";
        }

        public enum InOut {
            Input,
            Output,
            OutputThroughReturn
        }
    }
}