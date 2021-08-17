package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.fmi.FmiSimulationInstance;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.derivativeestimator.ScalarDerivativeEstimator;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

import java.util.*;

public class VariableStepConfigValue extends Value {

    private final Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances;
    private List<String> portNames;
    private List<StepVal> dataPoints;
    private Double currTime = 0.0;
    private Double stepSize = 0.0;
    private final Double maxStepSize;
    private StepValidationResult stepValidationResult;
    private final StepsizeCalculator stepsizeCalculator;
    private final Map<Fmi2ModelDescription.ScalarVariable, ScalarDerivativeEstimator> derivativeEstimators;

    public VariableStepConfigValue(Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances,
            Set<InitializationMsgJson.Constraint> constraints, StepsizeInterval stepsizeInterval, Double initSize, Double maxStepSize) throws InterpreterException {
        this.instances = instances;
        stepsizeCalculator = new StepsizeCalculator(constraints, stepsizeInterval, initSize, instances);
        Map<Fmi2ModelDescription.ScalarVariable, ScalarDerivativeEstimator> derEsts = new HashMap<>();
        instances.forEach((mi, fsi) -> fsi.config.scalarVariables.forEach(sv -> derEsts.put(sv, new ScalarDerivativeEstimator(2))));
        derivativeEstimators = derEsts;
        this.maxStepSize = maxStepSize;
    }

    public void initializePorts(List<String> portNames) {
        this.portNames = portNames;
    }

    public void addDataPoint(Double time, List<Value> portValues) {
        stepSize = time - currTime;
        currTime = time;
        dataPoints = convertValuesToDataPoint(portValues);
    }

    public double getStepSize() {
        Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Map<Integer, Double>>> currentDerivatives = new HashMap<>();
        Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Object>> currentPortValues = new HashMap<>();
        instances.forEach((mi, fsi) -> {
            Map<Fmi2ModelDescription.ScalarVariable, Map<Integer, Double>> derivatives = new HashMap<>();
            Map<Fmi2ModelDescription.ScalarVariable, Object> portValues = new HashMap<>();
            fsi.config.scalarVariables.forEach(
                    sv -> (dataPoints.stream().filter(dp -> (dp.getName().contains((mi.key + "." + mi.instanceName + "." + sv.name)))).findFirst())
                            .ifPresent(val -> {
                                ScalarDerivativeEstimator derEst = derivativeEstimators.get(sv);
                                derEst.advance(new Double[]{(Double) val.getValue(), null, null}, stepSize);
                                derivatives.putIfAbsent(sv, new HashMap<>() {{
                                    put(1, derEst.getFirstDerivative());
                                    put(2, derEst.getSecondDerivative());
                                }});

                                portValues.putIfAbsent(sv, val.getValue());
                            }));
            currentDerivatives.put(mi, derivatives);
            currentPortValues.put(mi, portValues);
        });

        return stepsizeCalculator.getStepsize(currTime, currentPortValues, currentDerivatives, maxStepSize);
    }

    public boolean isStepValid(Double nextTime, List<Value> portValues, boolean supportsRollBack) {
        stepValidationResult =
                stepsizeCalculator.validateStep(nextTime, mapModelInstancesToPortValues(convertValuesToDataPoint(portValues)), supportsRollBack);

        return stepValidationResult.isValid();
    }

    public boolean hasReducedStepSize() {
        if(stepValidationResult == null){
            throw new InterpreterException("'isStepValid' needs to be called before 'hasReducedStepSize'");
        }

        return stepValidationResult.hasReducedStepsize();
    }

    public Double getReducedStepSize() {
        if(stepValidationResult == null){
            throw new InterpreterException("'isStepValid' needs to be called before 'reducedStepSize'");
        }

        return stepValidationResult.getStepsize();
    }

    public void setEndTime(final Double endTime) {
        if (stepsizeCalculator != null) {
            stepsizeCalculator.setEndTime(endTime);
        }
    }

    private List<StepVal> convertValuesToDataPoint(List<Value> arrayValues) {
        List<StepVal> stepVals = new ArrayList<>();

        for (int i = 0; i < arrayValues.size(); i++) {
            Object value = arrayValues.get(i);

            if (value instanceof StringValue) {
                stepVals.add(new StepVal(portNames.get(i), ((StringValue) value).getValue()));
            } else if (value instanceof IntegerValue) {
                stepVals.add(new StepVal(portNames.get(i), ((IntegerValue) value).getValue()));
            } else if (value instanceof RealValue) {
                stepVals.add(new StepVal(portNames.get(i), ((RealValue) value).getValue()));
            } else if (value instanceof BooleanValue) {
                stepVals.add(new StepVal(portNames.get(i), ((BooleanValue) value).getValue()));
            } else if (value instanceof EnumerationValue) {
                stepVals.add(new StepVal(portNames.get(i), ((EnumerationValue) value).getValue()));
            }
        }
        return stepVals;
    }

    private Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Object>> mapModelInstancesToPortValues(
            List<StepVal> dataPointsToMap) {
        Map<ModelConnection.ModelInstance, Map<Fmi2ModelDescription.ScalarVariable, Object>> values = new HashMap<>();
        instances.forEach((mi, fsi) -> {
            Map<Fmi2ModelDescription.ScalarVariable, Object> portValues = new HashMap<>();
            fsi.config.scalarVariables.forEach(
                    sv -> (dataPointsToMap.stream().filter(dp -> (dp.getName().equals((mi.key + "." + mi.instanceName + "." + sv.name))))
                            .findFirst()).ifPresent(val -> {
                        // if key not absent something is wrong?
                        portValues.putIfAbsent(sv, val.getValue());

                    }));
            values.put(mi, portValues);
        });
        return values;
    }


    public static class StepVal {
        private String name;
        private Object value;

        public StepVal(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }


}
