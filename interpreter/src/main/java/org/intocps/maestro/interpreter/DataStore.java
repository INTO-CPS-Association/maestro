package org.intocps.maestro.interpreter;

import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DataStore {
    private static final DataStore dataStore = new DataStore();
    private Path sessionDirectory;
    private ISimulationEnvironment simulationEnvironment;

    private DataStore() {
        this.sessionDirectory = Paths.get("");
    }

    public static DataStore GetInstance() {
        return dataStore;
    }

    public Path getSessionDirectory() {
        return sessionDirectory;
    }

    public void setSessionDirectory(Path sessionDirectory) {
        this.sessionDirectory = sessionDirectory;
    }

    public ISimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public void setSimulationEnvironment(ISimulationEnvironment simulationEnvironment) {
        this.simulationEnvironment = simulationEnvironment;
    }
}
