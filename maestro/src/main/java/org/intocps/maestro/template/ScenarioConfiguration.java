package org.intocps.maestro.template;

import core.MasterModel;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;

import java.util.Map;

public class ScenarioConfiguration {
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final MasterModel masterModel;
    private final Map<String, Object> parameters;
    private final Double convergenceRelativeTolerance;
    private final Double convergenceAbsoluteTolerance;
    private final Integer convergenceAttempts;

    public ScenarioConfiguration(Fmi2SimulationEnvironment simulationEnvironment, MasterModel masterModel, Map<String, Object> parameters,
            Double convergenceRelativeTolerance, Double convergenceAbsoluteTolerance, Integer convergenceAttempts){
        this.simulationEnvironment = simulationEnvironment;
        this.masterModel = masterModel;
        this.parameters = parameters;
        this.convergenceRelativeTolerance = convergenceRelativeTolerance;
        this.convergenceAbsoluteTolerance = convergenceAbsoluteTolerance;
        this.convergenceAttempts = convergenceAttempts;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public MasterModel getMasterModel() {
        return masterModel;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Double getConvergenceRelativeTolerance() {
        return convergenceRelativeTolerance;
    }

    public Double getConvergenceAbsoluteTolerance() {
        return convergenceAbsoluteTolerance;
    }

    public Integer getConvergenceAttempts() {
        return convergenceAttempts;
    }
}
