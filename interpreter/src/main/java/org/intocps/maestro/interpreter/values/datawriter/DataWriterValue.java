package org.intocps.maestro.interpreter.values.datawriter;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;

import java.util.*;
import java.util.stream.Collectors;

public class DataWriterValue extends ModuleValue {

    public DataWriterValue(List<IDataListener> dataListeners) {
        super(createMembers(dataListeners));
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

    private static Map<String, Value> createMembers(List<IDataListener> IDataListeners) {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("writeHeader", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            List<StringValue> arrayValue = getArrayValue(fcargs.get(0), StringValue.class);
            List<String> headers = new ArrayList<>();
            for (StringValue strValue : arrayValue) {
                headers.add(strValue.getValue());
            }

            UUID uuid = UUID.randomUUID();
            IDataListeners.forEach(x -> x.writeHeader(uuid, headers));
            return new DataWriterConfigValue(uuid);
        }));
        componentMembers.put("writeDataPoint", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            Value id = fcargs.get(0).deref();
            if (id instanceof DataWriterConfigValue) {

                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> arrayValue = fcargs.stream().skip(1).map(Value::deref).collect(Collectors.toList());
                IDataListeners.forEach(dataListener -> dataListener.writeDataPoint(((DataWriterConfigValue) id).getUuid(), time, arrayValue));
            }

            return new VoidValue();
        }));
        componentMembers.put("close", new FunctionValue.ExternalFunctionValue(fcargs -> {
            IDataListeners.forEach(IDataListener::close);

            return new VoidValue();
        }));

        return componentMembers;
    }
}
