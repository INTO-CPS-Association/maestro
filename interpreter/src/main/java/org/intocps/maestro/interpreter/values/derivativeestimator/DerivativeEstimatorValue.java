package org.intocps.maestro.interpreter.values.derivativeestimator;
import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DerivativeEstimatorValue extends ModuleValue {

    public DerivativeEstimatorValue() {
        super(createMembers());
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("create", new FunctionValue.ExternalFunctionValue(fcargs -> {
            fcargs = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(fcargs, 3);

            List<Integer> indicesOfInterest =
                    ValueExtractionUtilities.getArrayValue(fcargs.get(0), UnsignedIntegerValue.class).stream().map(UnsignedIntegerValue::intValue).collect(
                            Collectors.toList());
            List<Integer> derivativeOrders =
                    ValueExtractionUtilities.getArrayValue(fcargs.get(1), UnsignedIntegerValue.class).stream().map(UnsignedIntegerValue::intValue).collect(
                    Collectors.toList());
            List<Integer> providedDerivativeOrders =
                    ValueExtractionUtilities.getArrayValue(fcargs.get(2), UnsignedIntegerValue.class).stream().map(UnsignedIntegerValue::intValue).collect(
                    Collectors.toList());

            return new DerivativeEstimatorInstanceValue(indicesOfInterest, derivativeOrders, providedDerivativeOrders);
        }));

        return componentMembers;
    }
}
