package org.intocps.maestro.interpreter.values.derivativeestimator;

import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;

import java.util.*;
import java.util.stream.Collectors;

public class DerivativeEstimatorInstanceValue extends ModuleValue {
    private static final Map<Integer, ScalarDerivativeEstimator> estimators = new HashMap<>(); //Keys corresponds to the values in indicesOfInterest

    public DerivativeEstimatorInstanceValue(List<Integer> indicesOfInterest, List<Integer> derivativeOrder, List<Integer> providedDerivativesOrder) {
        super(createMembers(indicesOfInterest, derivativeOrder, providedDerivativesOrder));
        for (int i = 0; i < indicesOfInterest.size(); i++) {
            estimators.put(indicesOfInterest.get(i), new ScalarDerivativeEstimator(derivativeOrder.get(i)));
        }
    }

    /**
     * Dont rollback FMU provided values
     *
     * @param indicesOfInterest        The indices of relevant data in sharedData and sharedDataDerivatives that is passed to 'estimate'
     * @param derivativeOrders         The derivative order that is to be estimated for each data of interest
     * @param providedDerivativesOrder Any derivative order that is already provided and thus should not be estimated.
     * @return Component members
     */
    private static Map<String, Value> createMembers(List<Integer> indicesOfInterest, List<Integer> derivativeOrders,
            List<Integer> providedDerivativesOrder) {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("estimate", new FunctionValue.ExternalFunctionValue(fcargs -> {
            fcargs = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(fcargs, 3);

            Double stepSize = ValueExtractionUtilities.getValue(fcargs.get(0), RealValue.class).getValue();
            List<Value> sharedDataDerivatives = ValueExtractionUtilities.getArrayValue(fcargs.get(2), Value.class);

            for (int i = 0; i < indicesOfInterest.size(); i++) {
                Integer indexOfInterest = indicesOfInterest.get(i);
                double dataOfInterest = ValueExtractionUtilities.getArrayValue(fcargs.get(1), RealValue.class).get(indexOfInterest).getValue();

                List<Double> dataDerivativesOfInterest =
                        ValueExtractionUtilities.getArrayValue(sharedDataDerivatives.get(indexOfInterest), Value.class).stream().map(v -> {
                            if (v instanceof RealValue) {
                                return ((RealValue) v).getValue();
                            } else {
                                return null;
                            }
                        }).collect(Collectors.toList());

                // provided has to be of size 3 for the estimator.
                Double[] provided = {dataOfInterest, null, null};
                for (int derOrderIndex = 1; derOrderIndex <= providedDerivativesOrder.get(i); derOrderIndex++) {
                    provided[derOrderIndex] = dataDerivativesOfInterest.get(derOrderIndex - 1);
                }
                estimators.get(indexOfInterest).advance(provided, stepSize);

                for (int derOrder = providedDerivativesOrder.get(i); derOrder < derivativeOrders.get(i); derOrder++) {
                    ValueExtractionUtilities.getArrayValue(sharedDataDerivatives.get(indexOfInterest), Value.class)
                            .set(derOrder, new RealValue(estimators.get(indexOfInterest).getDerivative(derOrder + 1)));
                }

            }
            return new BooleanValue(true);

        }));
        componentMembers.put("rollback", new FunctionValue.ExternalFunctionValue(fcargs -> {
            fcargs = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(fcargs, 1);

            List<Value> sharedDataDerivativesForIndex = ValueExtractionUtilities.getArrayValue(fcargs.get(0), Value.class);

            estimators.forEach((indexOfInterest, estimator) -> {
                estimator.rollback();

                for (int derOrder = providedDerivativesOrder.get(indicesOfInterest.indexOf(indexOfInterest));
                        derOrder < estimators.get(indexOfInterest).getOrder(); derOrder++) {
                    Double der = estimator.getDerivative(derOrder + 1);
                    ValueExtractionUtilities.getArrayValue(sharedDataDerivativesForIndex.get(indexOfInterest), Value.class)
                            .set(derOrder, der != null ? new RealValue(der) : new NullValue());
                }
            });

            return new BooleanValue(true);
        }));

        return componentMembers;
    }

}
