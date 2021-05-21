package org.intocps.maestro.template;

import core.MasterModel;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;

public class ScenarioConfiguration {
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final MasterModel masterModel;

    public ScenarioConfiguration(Fmi2SimulationEnvironment simulationEnvironment, MasterModel masterModel){
        this.simulationEnvironment = simulationEnvironment;
        this.masterModel = masterModel;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public MasterModel getMasterModel() {
        return masterModel;
    }
}
