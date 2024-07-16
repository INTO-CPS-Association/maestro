package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.ValueExtractionUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MathValue extends ExternalModuleValue<Object> {
    public MathValue() {
        super(createMembers(), null);
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("isClose", new FunctionValue.ExternalFunctionValue(fcArgs -> {
            double a = ((NumericValue) fcArgs.get(0).deref()).realValue();
            double b = ((NumericValue) fcArgs.get(1).deref()).realValue();
            double absoluteTolerance = ((NumericValue) fcArgs.get(2).deref()).realValue();
            double relativeTolerance = ((NumericValue) fcArgs.get(3).deref()).realValue();

            return new BooleanValue(Math.abs(a - b) <= (absoluteTolerance + relativeTolerance * Math.abs(b)));
        }));
        componentMembers.put("min", new FunctionValue.ExternalFunctionValue(args -> {

            if (args.get(0).deref() instanceof IntegerValue) {
                return new IntegerValue(Math.min(((IntegerValue) args.get(0).deref()).intValue(), ((IntegerValue) args.get(1).deref()).intValue()));

            } else {
                return new RealValue(Math.min(((RealValue) args.get(0).deref()).realValue(), ((RealValue) args.get(1).deref()).realValue()));
            }

        }));
        componentMembers.put("minRealFromArray", new FunctionValue.ExternalFunctionValue(args -> {

            if (args.get(0).deref() instanceof ArrayValue) {
                List<Value> valueArray = ValueExtractionUtilities.getArrayValue(args.get(0), Value.class);
                if (valueArray.size() < 1) {
                    return new IntegerValue(0);
                } else if (valueArray.get(0).deref() instanceof RealValue) {
                    RealValue minDoubleSize = (RealValue) valueArray.get(0);
                    for (int i = 1; i < valueArray.size(); i++) {
                        if (((RealValue) valueArray.get(i)).getValue() < minDoubleSize.getValue()) {
                            minDoubleSize = (RealValue) valueArray.get(i);
                        }
                    }
                    return minDoubleSize;
                } else if (valueArray.get(0).deref() instanceof IntegerValue) {
                    IntegerValue minIntSize = (IntegerValue) valueArray.get(0);
                    for (int i = 1; i < valueArray.size(); i++) {
                        if (((IntegerValue) valueArray.get(i)).getValue() < minIntSize.getValue()) {
                            minIntSize = (IntegerValue) valueArray.get(i);
                        }
                    }
                    return minIntSize;
                } else {
                    throw new InterpreterException("Array values are not of type real.");
                }
            } else {
                throw new InterpreterException("Value passed is not an array.");
            }
        }));

        return componentMembers;
    }
}
