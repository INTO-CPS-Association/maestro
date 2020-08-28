package org.intocps.maestro.interpreter.values;

import java.util.HashMap;
import java.util.Map;

public class MathValue extends ExternalModuleValue {
    public MathValue() {
        super(createMembers(), null);
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("isClose", new FunctionValue.ExternalFunctionValue(fcArgs -> {
            Double a = ((RealValue) fcArgs.get(0).deref()).realValue();
            Double b = ((RealValue) fcArgs.get(1).deref()).realValue();
            Double absoluteTolerance = ((RealValue) fcArgs.get(2).deref()).realValue();
            Double relativeTolerance = ((RealValue) fcArgs.get(3).deref()).realValue();

            return new BooleanValue(Math.abs(a - b) <= (absoluteTolerance + relativeTolerance * Math.abs(b)));
        }));
        return componentMembers;
    }
}
