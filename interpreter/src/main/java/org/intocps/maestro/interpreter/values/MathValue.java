package org.intocps.maestro.interpreter.values;

import java.util.HashMap;
import java.util.Map;

public class MathValue extends ExternalModuleValue<Object> {
    public MathValue() {
        super(createMembers(), null);
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("isClose", new FunctionValue.ExternalFunctionValue(fcArgs -> {
            double a = ((RealValue) fcArgs.get(0).deref()).realValue();
            double b = ((RealValue) fcArgs.get(1).deref()).realValue();
            double absoluteTolerance = ((RealValue) fcArgs.get(2).deref()).realValue();
            double relativeTolerance = ((RealValue) fcArgs.get(3).deref()).realValue();

            return new BooleanValue(Math.abs(a - b) <= (absoluteTolerance + relativeTolerance * Math.abs(b)));
        }));
        componentMembers.put("min", new FunctionValue.ExternalFunctionValue(args -> {

            if (args.get(0).deref() instanceof IntegerValue) {
                return new IntegerValue(Math.min(((IntegerValue) args.get(0).deref()).intValue(), ((IntegerValue) args.get(1).deref()).intValue()));

            } else {
                return new RealValue(Math.min(((RealValue) args.get(0).deref()).realValue(), ((RealValue) args.get(1).deref()).realValue()));
            }

        }));
        return componentMembers;
    }
}
