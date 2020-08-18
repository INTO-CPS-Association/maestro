package org.intocps.maestro.interpreter.values.csv;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvFileValue extends ExternalModuleValue<PrintWriter> {
    public CsvFileValue(PrintWriter writer) {
        super(createCsvMembers(writer), writer);
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

    private static Map<String, Value> createCsvMembers(final PrintWriter writer) {

        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("writeHeader", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 1);

            List<StringValue> headers = getArrayValue(fcargs.get(0), StringValue.class);

            writer.println(Stream.concat(Stream.of("time"), headers.stream().map(StringValue::getValue)).collect(Collectors.joining(",")));

            return new VoidValue();
        }));
        componentMembers.put("writeRow", new FunctionValue.ExternalFunctionValue(fcargs -> {

            checkArgLength(fcargs, 2);

            double time = getDouble(fcargs.get(0));

            ArrayValue<Value> arrayValue = (ArrayValue<Value>) fcargs.get(1).deref();

            List<Object> data = new Vector<>();
            data.add(time);

            for (Value d : arrayValue.getValues()) {
                if (d instanceof IntegerValue) {
                    data.add(((IntegerValue) d).intValue());
                }
                if (d instanceof RealValue) {
                    data.add(((RealValue) d).realValue());
                }
                if (d instanceof BooleanValue) {
                    data.add(((BooleanValue) d).getValue());
                }
            }

            writer.println(data.stream().map(Object::toString).collect(Collectors.joining(",")));

            return new VoidValue();
        }));
        return componentMembers;

    }

}
