package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.interpreter.values.Value;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.List;
import java.util.Map;

public class VariableStepConfigValue extends Value {

    private final Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances;
    private List<String> portNames;

    public VariableStepConfigValue(Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances) {
        this.instances = instances;
    }

    public void initializePorts(List<String> portNames) {
        this.portNames = portNames;
    }

    public void addDataPoint(Double time, Map<ModelDescription.Types, Object> dataPoint) {
    }

    public double getStepSize() {
        return 0;
    }

    public static class VarStepValue {
        public String name;
        public Object value;

    }


}
