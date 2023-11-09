package org.intocps.maestro.interpreter.values.utilities;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;

import java.util.*;
import java.util.stream.Collectors;

public class ByteArrayArrayValue extends ExternalModuleValue<List<List<ByteValue>>> {
    public ByteArrayArrayValue(int size) {

        this(create(size));

    }

    ByteArrayArrayValue(Map.Entry<Map<String, Value>, List<List<ByteValue>>> d) {
        super(d.getKey(), d.getValue());
    }

    public static Map.Entry<Map<String, Value>, List<List<ByteValue>>> create(int size) {
        List<List<ByteValue>> list = new ArrayList<>(size);
        return Map.entry(createMembers(list), list);
    }


    public static <T extends Value> List<T> getArrayValue(Value value, Class<T> clz) {

        value = value.deref();

        if (value instanceof ArrayValue) {

            ArrayValue array = (ArrayValue) value;
            if (((ArrayValue) value).getValues().isEmpty()) {
                return Collections.emptyList();
            }

            if (!clz.isAssignableFrom(array.getValues().get(0).getClass())) {
                throw new InterpreterException("Array not containing the right type");
            }

            return array.getValues();
        }
        throw new InterpreterException("Value is not an array");
    }

    public static String getString(Value value) {

        value = value.deref();

        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        throw new InterpreterException("Value is not string");
    }

    public static double getDouble(Value value) {

        value = value.deref();

        if (value instanceof RealValue) {
            return ((RealValue) value).getValue();
        }
        throw new InterpreterException("Value is not double");
    }


    private static Map<String, Value> createMembers(final List<List<ByteValue>> list) {

        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("set", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 2);

            List<ByteValue> from = getArrayValue(fcargs.get(1), ByteValue.class);

            int index = ((NumericValue) fcargs.get(0).deref()).intValue();
            list.add(index, from);

            return new VoidValue();


        }));

        componentMembers.put("get", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            int index = ((NumericValue) fcargs.get(0).deref()).intValue();
            return new ArrayValue<>(list.get(index));

        }));

        componentMembers.put("getArraySizes", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 0);

            return new ArrayValue<>(list.stream().map(List::size).map(IntegerValue::new).collect(Collectors.toList()));

        }));

        return componentMembers;

    }
}
