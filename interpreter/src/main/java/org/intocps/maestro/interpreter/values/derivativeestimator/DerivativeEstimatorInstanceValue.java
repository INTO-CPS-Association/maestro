package org.intocps.maestro.interpreter.values.derivativeestimator;

import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DerivativeEstimatorInstanceValue extends ExternalModuleValue {

    HashMap<Integer, DerivativesForVariable> derivatives;
    int variablesCount;

    public DerivativeEstimatorInstanceValue(int variablesCount_, HashMap<Integer, DerivativesForVariable> derivatives) {
        super(createMembers(variablesCount_, derivatives), null);
        this.variablesCount = variablesCount_;
        this.derivatives = derivatives;
    }

    /**
     * @param variables
     * @param orders
     * @param provided       describes how many derivatives of the variable are provided by the FMU itself. 1 is first derivative, 2 is second derivative
     *                       etc. 0 means that no derivatives is provided, only the variable value.
     * @param variablesCount
     */
    public static DerivativeEstimatorInstanceValue createDerivativeEstimatorInstanceValue(List<StringValue> variables, List<IntegerValue> orders,
            List<IntegerValue> provided, IntegerValue variablesCount) {
        int variablesCount_ = variablesCount.getValue();
        HashMap<Integer, DerivativesForVariable> derivatives = new HashMap<>();
        for (int i = 0; i < variablesCount_; i++) {
            String s = variables.get(i).getValue();
            Integer order = orders.get(i).getValue();
            Integer provided_ = provided.get(i).getValue();
            ScalarDerivativeEstimator estimator = new ScalarDerivativeEstimator(order);
            derivatives.put(i, new DerivativesForVariable(order, provided_, i, s, estimator));
        }
        return new DerivativeEstimatorInstanceValue(variablesCount_, derivatives);
    }

    private static Map<String, Value> createMembers(int size, HashMap<Integer, DerivativesForVariable> derivatives) {
        Map<String, Value> componentMembers = new HashMap<>();

        // calculate(real time, real[] values, ref real[] derivativesOutput)
        // The values are related to the arguments: variables and provided in the constructor.
        // For example, if variables=["x","y"] and provided=[2,1], then values consist of: [x, derx, derderx, y, dery].
        componentMembers.put("calculate", new FunctionValue.ExternalFunctionValue(fcargs -> {
            Double time = ValueExtractionUtilities.getValue(fcargs.get(0), RealValue.class).getValue();

            // Group values according to variables.
            List<Double> values = ValueExtractionUtilities.getArrayValue(fcargs.get(1), RealValue.class).stream().map(RealValue::getValue)
                    .collect(Collectors.toList());

            List<RealValue> outputReference = ((ArrayValue<RealValue>) fcargs.get(2).deref()).getValues();

            Map<Integer, Double[]> variableIndexToProvidedValues = new HashMap<>();

            // Variable used to control iteration of values argument
            int valuesIndex = 0;

            // Variable used to control insertion into outputReference array
            int outputReferenceIndex = 0;

            for (int variableIterator = 0; variableIterator < size; variableIterator++) {
                DerivativesForVariable derivativesForVariable = derivatives.get(variableIterator);

                // Extract values related to variable
                int nextValuesIndex = valuesIndex + derivativesForVariable.sizeOfProvidedValues;
                // provided has to be of size 3 for the estimator.
                Double[] provided = values.subList(valuesIndex, nextValuesIndex).toArray(new Double[3]);
                variableIndexToProvidedValues.put(variableIterator, provided);
                derivativesForVariable.estimator.advance(provided, time);

                double[] calculatedDerivatives = new double[derivativesForVariable.order];

                // j-for loop is for retrieving derivatives
                // k-for loop is for indexing them into calculatedDerivatives
                for (int derivativeOrder = 1; derivativeOrder <= derivativesForVariable.order; derivativeOrder++, outputReferenceIndex++) {
                    outputReference.add(outputReferenceIndex, new RealValue(derivativesForVariable.estimator.getDerivative(derivativeOrder)));
                }

                valuesIndex = nextValuesIndex;
            }
            return new BooleanValue(true);
        }));

        componentMembers.put("rollback", new FunctionValue.ExternalFunctionValue(fcargs -> {
            derivatives.values().forEach(l -> l.estimator.rollback());
            return new BooleanValue(true);
        }));

        return componentMembers;
    }

    public static class DerivativesForVariable {
        public final Integer order;
        public final Integer provided;
        public final Integer index;
        public final String variable;
        public final ScalarDerivativeEstimator estimator;
        public final int sizeOfProvidedValues;

        public DerivativesForVariable(Integer order, Integer provided, Integer index, String variable, ScalarDerivativeEstimator estimator) {
            this.order = order;
            this.provided = provided;
            this.index = index;
            this.variable = variable;
            this.estimator = estimator;
            // + 1 is due to provided is related conceptually to the derivates provided. However, the original variable is always provided. I.e
            // Example: For x, derx and derderx provided will be 2, yet three values are sent.
            this.sizeOfProvidedValues = this.provided + 1;
        }
    }

}



