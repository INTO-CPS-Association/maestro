package org.intocps.maestro.interpreter.values;

import java.util.HashMap;
import java.util.Map;

public class BooleanLogicValue extends ExternalModuleValue<Object> {
    public BooleanLogicValue() {
        super(createMembers(), null);
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("allTrue", new FunctionValue.ExternalFunctionValue(fcArgs -> {
            return new BooleanValue(fcArgs.stream().map(x -> ((BooleanValue) x.deref()).getValue()).allMatch(x -> x));
        }));
        componentMembers.put("allFalse", new FunctionValue.ExternalFunctionValue(args -> {
            return new BooleanValue(args.stream().map(x -> ((BooleanValue) x.deref()).getValue()).allMatch(x -> !x));
        }));
        return componentMembers;
    }
}
