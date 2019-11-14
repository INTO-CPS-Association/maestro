package org.intocps.orchestration.coe.webapi.services;

import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.LogVariablesContainer;
import org.intocps.orchestration.coe.webapi.controllers.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class CoeService {
    private final static Logger logger = LoggerFactory.getLogger(CoeService.class);
    private boolean simulating = false;
    private double startTime = 0d;
    private double endTime = 0d;
    private Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs;
    private Map<String, List<ModelDescription.LogCategory>> availableDebugLoggingCategories;
    private Coe coe;
    private Coe.CoeSimulationHandle simulationHandle = null;

    public Coe get() {

        if (coe != null) {
            return coe;
        }

        String session = UUID.randomUUID().toString();
        File root = new File(session);
        if (root.mkdirs()) {
            logger.error("Could not create session directory for COE: {}", root.getAbsolutePath());
        }

        this.coe = new Coe(root);
        return coe;
    }

    public void initialize(Map<String, URI> fmus, CoSimStepSizeCalculator stepSizeCalculator, Double endTime, List<ModelParameter> parameters,
            List<ModelConnection> connections, LogVariablesContainer logVariables, List<ModelParameter> inputs,
            Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs) throws Exception {

        //TODO insert what ever is needed to connect the FMU to handle single FMU simulations
        //construct fmu for external env representation as:
        // env.outputs = body.inputs
        // env.inputs = body.requested_outputs
        // update connections with this connection

        this.startTime = 0d;
        this.endTime = endTime;
        this.outputs = outputs;

        if (connections == null || !connections.isEmpty()) {
            throw new Exception("No connections provided");
        }

        try {
            coe.getConfiguration().isStabalizationEnabled = false;
            coe.getConfiguration().global_absolute_tolerance = 0d;
            coe.getConfiguration().global_relative_tolerance = 0d;
            coe.getConfiguration().loggingOn = logVariables != null && logVariables.getLogVariables().values().stream().mapToLong(Set::size)
                    .sum() > 0;
            coe.getConfiguration().visible = false;
            coe.getConfiguration().parallelSimulation = false;
            coe.getConfiguration().simulationProgramDelay = false;
            coe.getConfiguration().hasExternalSignals = true;//TODO maybe?

            this.availableDebugLoggingCategories = coe.initialize(fmus, connections, parameters, stepSizeCalculator, logVariables);

            logger.trace("Initialization completed obtained the following logging categories: {}", availableDebugLoggingCategories.entrySet().stream()
                    .map(map -> map.getKey() + "=" + map.getValue().stream().map(c -> c.name).collect(Collectors.joining(",", "[", "]")))
                    .collect(Collectors.joining(",")));

        } catch (Exception e) {
            logger.error("Internal error in initialization", e);
            throw new InitializationException(e.getMessage(), e);
        }

    }

    public void simulate(Map<ModelConnection.ModelInstance, List<String>> debugLoggingCategories, boolean reportProgress, double liveLogInterval) {
        get().simulate(startTime, endTime, debugLoggingCategories, reportProgress, liveLogInterval);
    }

    public void configureSimulationDeltaStepping(Map<ModelConnection.ModelInstance, List<String>> debugLoggingCategories, boolean reportProgress,
            double liveLogInterval) {
        if (simulationHandle == null) {
            this.simulationHandle = get().getSimulateControlHandle(startTime, endTime, debugLoggingCategories, reportProgress, liveLogInterval);
        }
    }

    public void simulate(double delta) throws SimulatorNotConfigured {

        if (simulationHandle == null) {
            throw new SimulatorNotConfigured("Simulation handle not configured");
        }
        if (!this.simulating) {
            this.simulationHandle.preSimulation();
            this.simulating = true;
        }
        this.simulationHandle.simulate(delta);

    }

    public void stop() {
        if (simulating) {
            this.simulationHandle.postSimulation();
        }
        get().stopSimulation();
    }

    public void simulate(double delta, List<ModelParameter> inputs) throws SimulatorNotConfigured, SimulatorInputNotRegonized {

        if (simulationHandle == null) {
            configureSimulationDeltaStepping(new HashMap<>(), false, 0d);
        }

        simulate(delta);
    }

    public class SimulatorNotConfigured extends Exception {
        public SimulatorNotConfigured(String message) {
            super(message);
        }
    }

    public class SimulatorInputNotRegonized extends Exception {
        public SimulatorInputNotRegonized(String message) {
            super(message);
        }
    }
}
