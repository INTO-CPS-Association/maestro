package org.intocps.maestro.webapi.controllers;

import java.io.File;

public class SessionLogic {
    public final File rootDirectory;
    private Maestro2SimulationController.InitializationData initializationData;
    private Maestro2SimulationController.SimulateRequestBody simulateRequestBody;

    public SessionLogic(File rootDirectory) {
        this.rootDirectory = rootDirectory;

    }

    public Maestro2SimulationController.InitializationData getInitializationData() {
        return initializationData;
    }

    public void setInitializationData(Maestro2SimulationController.InitializationData initializationData) {
        this.initializationData = initializationData;
    }

    public Maestro2SimulationController.SimulateRequestBody getSimulateRequestBody() {
        return simulateRequestBody;
    }

    public void setSimulateRequestBody(Maestro2SimulationController.SimulateRequestBody simulateRequestBody) {
        this.simulateRequestBody = simulateRequestBody;
    }
}
