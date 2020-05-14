package org.intocps.maestro.webapi.controllers;

import java.io.File;

public class SessionLogic {
    public final File rootDirectory;
    private MaestroSimulationController.InitializationData initializationData;
    private MaestroSimulationController.SimulateRequestBody simulateRequestBody;

    public SessionLogic(File rootDirectory) {
        this.rootDirectory = rootDirectory;

    }

    public MaestroSimulationController.InitializationData getInitializationData() {
        return initializationData;
    }

    public void setInitializationData(MaestroSimulationController.InitializationData initializationData) {
        this.initializationData = initializationData;
    }

    public MaestroSimulationController.SimulateRequestBody getSimulateRequestBody() {
        return simulateRequestBody;
    }

    public void setSimulateRequestBody(MaestroSimulationController.SimulateRequestBody simulateRequestBody) {
        this.simulateRequestBody = simulateRequestBody;
    }
}
