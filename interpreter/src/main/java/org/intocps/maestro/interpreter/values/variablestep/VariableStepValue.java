package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;
import java.util.*;
import java.util.stream.Collectors;
import org.intocps.maestro.interpreter.values.utilities.ArrayUtilValue;

public class VariableStepValue extends ModuleValue {

    public VariableStepValue(String configuration) {
        super(createMembers());
    }


    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("writeHeader", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            List<StringValue> arrayValue = ArrayUtilValue.getArrayValue(fcargs.get(0), StringValue.class);
            List<String> headers = new ArrayList<>();
            for (StringValue strValue : arrayValue) {
                headers.add(strValue.getValue());
            }

            UUID uuid = UUID.randomUUID();
            //IDataListeners.forEach(x -> x.writeHeader(uuid, headers));
            return new VariableStepConfigValue(uuid);
        }));
        componentMembers.put("writeDataPoint", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {

                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> arrayValue = fcargs.stream().skip(2).map(Value::deref).collect(Collectors.toList());
                //IDataListeners.forEach(dataListener -> dataListener.writeDataPoint(((DataWriterConfigValue) id).getUuid(), time, arrayValue));
            }

            return new VoidValue();
        }));

        return componentMembers;
    }

}
