package org.intocps.maestro.interpreter.values.derivativeestimator;

import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DerivativeEstimatorValue extends ExternalModuleValue {
    HashMap<UUID, DerivativeEstimatorInstance> instances = new HashMap<>();

    public DerivativeEstimatorValue() {
        super(createMembers(), null);
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("variablesOfInterest", new FunctionValue.ExternalFunctionValue(fcargs -> {

            fcargs = fcargs.stream().map(Value::deref).collect(Collectors.toList());

            checkArgLength(fcargs, 4);

            List<StringValue> variables = ValueExtractionUtilities.getArrayValue(fcargs.get(0), StringValue.class);
            List<IntegerValue> order = ValueExtractionUtilities.getArrayValue(fcargs.get(1), IntegerValue.class);
            List<IntegerValue> provided = ValueExtractionUtilities.getArrayValue(fcargs.get(2), IntegerValue.class);
            IntegerValue size = ValueExtractionUtilities.getValue(fcargs.get(3), IntegerValue.class);

            DerivativeEstimatorInstanceValue de =
                    DerivativeEstimatorInstanceValue.createDerivativeEstimatorInstanceValue(variables, order, provided, size);

            return de;
        }));

        componentMembers.put("calculateDerivatives", new FunctionValue.ExternalFunctionValue(fcargs -> {
            fcargs = fcargs.stream().map(Value::deref).collect(Collectors.toList());

            checkArgLength(fcargs, 4);

            DerivativeEstimatorInstanceValue de = ValueExtractionUtilities.getValue(fcargs.get(0), DerivativeEstimatorInstanceValue.class);

            return null;
        }));

        return componentMembers;
    }
}
