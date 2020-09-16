package org.intocps.maestro.interpreter;

import org.intocps.maestro.interpreter.values.ArrayValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Collections;
import java.util.List;

public class ValueExtractionUtilities {
    public static <T extends Value> T getValue(Value value, Class<T> clz) {

        Value dereffed = value.deref();

        if (clz.isAssignableFrom(dereffed.getClass())) {
            return clz.cast(dereffed);
        }

        throw new InterpreterException("Value is not the right type");
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
}
