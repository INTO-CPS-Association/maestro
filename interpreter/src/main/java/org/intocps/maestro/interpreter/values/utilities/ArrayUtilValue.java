package org.intocps.maestro.interpreter.values.utilities;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;

import java.util.*;

public class ArrayUtilValue extends ExternalModuleValue<Object> {
    public ArrayUtilValue() {
        super(createMembers(), null);
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

    private static Map<String, Value> createMembers() {

        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("copyRealArray", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 5);

            List<RealValue> from = getArrayValue(fcargs.get(0), RealValue.class);

            int fromIndex = ((NumericValue) fcargs.get(1).deref()).intValue();
            int fromCount = ((NumericValue) fcargs.get(2).deref()).intValue();

            UpdatableValue to = (UpdatableValue) fcargs.get(3);
            int toStartIndex = ((NumericValue) fcargs.get(4).deref()).intValue();

            ArrayValue<NumericValue> toSource = (ArrayValue<NumericValue>) to.deref();

            NumericValue[] values = new NumericValue[toSource.getValues().size()];
            for (int i = 0; i < fromCount; i++) {
                values[toStartIndex + i] = from.get(i + fromIndex);
            }

            for (int i = 0; i < toStartIndex; i++) {
                values[i] = toSource.getValues().get(i);
            }
            for (int i = toStartIndex + fromCount; i < toSource.getValues().size(); i++) {
                values[i] = toSource.getValues().get(i);
            }


            ArrayValue<NumericValue> newValue = new ArrayValue<>(Arrays.asList(values));


            to.setValue(newValue);


            return new VoidValue();
        }));

        componentMembers.put("createByteArrayArray", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);
            int size = ((NumericValue) fcargs.get(0).deref()).intValue();
            return new ByteArrayArrayValue(size);
        }));

        return componentMembers;

    }
}
