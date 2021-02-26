package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.interpreter.values.Value;
import org.intocps.orchestration.coe.AbortSimulationException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class VariableStepConfigValue extends Value {

    private final Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances;
    private List<String> portNames;
    private List<StepVal> dataPoint;
    private Double time;
    private StepsizeCalculator stepsizeCalculator;

    public VariableStepConfigValue(Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances,
            Set<InitializationMsgJson.Constraint> constraints, StepsizeInterval stepsizeInterval, Double initSize) throws AbortSimulationException {
        this.instances = instances;
        stepsizeCalculator = new StepsizeCalculator(constraints, stepsizeInterval, initSize, instances);
    }

    public void initializePorts(List<String> portNames) {
        this.portNames = portNames;
    }

    public void addDataPoint(Double time, List<StepVal> dataPoint) {
        this.time = time;
        this.dataPoint = dataPoint;
    }

    public double getStepSize() {
        Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, Object>> currentValues = new HashMap<>();
        Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, Map<Integer, Double>>> currentDerivatives = new HashMap<>();
        instances.forEach((mi, fsi) -> {
            Map<ModelDescription.ScalarVariable, Object> scalarValues = new HashMap<>();
            Map<ModelDescription.ScalarVariable, Map<Integer, Double>> derivatives = new HashMap<>();
            fsi.config.scalarVariables.forEach(sv -> {
                Optional<StepVal> stepVal = dataPoint.stream()
                        .filter(dp -> (dp.getName().contains((mi.key + "." + mi.instanceName + "." + sv.name))))
                        .findFirst();
                stepVal.ifPresent(val -> {
                    scalarValues.put(sv, val.getValue());

                    try {
                        List<ModelDescription.ScalarVariable> ders = fsi.config.modelDescription.getDerivatives();
                    } catch (XPathExpressionException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    if (!derivatives.containsKey(sv)) {
                        derivatives.put(sv, new HashMap<>() {{
                            put(0, 0.0);
                        }});
                    }
                    else{
                        derivatives.get(sv).put(0, 0.0);
                    }
                });
            });
            currentDerivatives.put(mi,derivatives);
            currentValues.put(mi, scalarValues);
        });

        return stepsizeCalculator.getStepsize(time, currentValues, currentDerivatives, null);
    }

    public void setEndTime(final Double endTime) {
        if (stepsizeCalculator != null) {
            stepsizeCalculator.setEndTime(endTime);
        }
    }

    public List<String> getPorts() {
        return this.portNames;
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
