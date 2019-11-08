package org.intocps.orchestration.coe.webapi.services;

import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.FmuFactory;
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
    private Map<String, List<ModelDescription.LogCategory>> availableDebugLoggingCategories;
    private Map<String, List<String>> requestedDebugLoggingCategories;
    private Coe coe;
    private Coe.CoeSimulationHandle simulationHandle = null;

    public CoeService(Coe coe) {
        this.coe = coe;
    }

    public CoeService() {
        String session = UUID.randomUUID().toString();
        File root = new File(session);
        if (root.mkdirs()) {
            logger.error("Could not create session directory for COE: {}", root.getAbsolutePath());
        }

        this.coe = new Coe(root);
    }

    private static List<ModelConnection> createEnvConnection(List<ModelDescription.ScalarVariable> fromVariables, ModelConnection.ModelInstance fromInstance, ModelConnection.ModelInstance toInstance) {

        return fromVariables.stream().map(scalarVariable -> {
            ModelConnection.Variable from = new ModelConnection.Variable(fromInstance, scalarVariable.name);
            ModelConnection.Variable to = new ModelConnection.Variable(toInstance, scalarVariable.name);
            ModelConnection connection = new ModelConnection(from, to);
            return connection;
        }).collect(Collectors.toList());
    }

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
                           List<ModelConnection> connections, Map<String, List<String>> requestedDebugLoggingCategories, List<ModelParameter> inputs,
                           Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs) throws Exception {

        //FIXME insert what ever is needed to connect the FMU to handle single FMU simulations.
        // - DONE Construct an FMU for external environment representation. This will be called environment FMU:
        // - DONE Update fmus with the environment FMU
        // - DONE The outputs of the environment fmu are the inputs of the single FMU.
        // - DONE The inputs of the environment FMU are the outputs of the single FMU.
        // - DONE update connections with this connection
        // - INPROGRESS Create test with fake coe
        // - Create custom FMU factory

        if (fmus.size() == 1) {
            IFmu fmu = FmuFactory.create(get().getResultRoot(), fmus.entrySet().iterator().next().getValue());
            ModelDescription modelDescription = new ModelDescription(fmu.getModelDescription());
            List<ModelDescription.ScalarVariable> modelDescScalars = modelDescription.getScalarVariables();
            EnvironmentFMU environmentFMU = EnvironmentFMU.CreateEnvironmentFMU("environmentFMU", "environmentInstance");

            fmus.put(environmentFMU.key, new URI("environment://".concat(environmentFMU.fmuName)));
            // At this point there is 1 FMU and 1 instance
            if (outputs.entrySet().size() == 1) {

                Map.Entry<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> singleFmuInstanceOutputs = outputs.entrySet().iterator().next();
                List<ModelDescription.ScalarVariable> singleFmuCorrelatedOutputs = singleFmuInstanceOutputs.getValue().stream().map(sv -> {
                    Optional<ModelDescription.ScalarVariable> correlatedScalars = modelDescScalars.stream().filter(svMd -> svMd.name.equals(sv.name)).findFirst();
                    return correlatedScalars.get();
                }).collect(Collectors.toList());
                environmentFMU.calculateInputs(singleFmuCorrelatedOutputs);
                environmentFMU.calculateOutputs(modelDescription);

                // Using a single variable as source for both to and from variable name is valid due to the invariant on calculated inputs and outputs.
                ModelConnection.ModelInstance environmentFMUInstance = new ModelConnection.ModelInstance(environmentFMU.key, environmentFMU.instanceName);
                //connections for: environment FMU outputs -> single FMU inputs
                List<ModelConnection> envFmuOutputsToSingleFmuInputs = createEnvConnection(environmentFMU.getOutputs(), environmentFMUInstance, singleFmuInstanceOutputs.getKey());
                // connections are expected to be null at this point since there is 1 FMU and 1 instance.
                connections = envFmuOutputsToSingleFmuInputs;
                //connections for: single FMU outputs -> environment FMU inputs
                List<ModelConnection> singleFmuOutputsToEnvFmuInputs = createEnvConnection(singleFmuCorrelatedOutputs, singleFmuInstanceOutputs.
                        getKey(), environmentFMUInstance);
                connections.addAll(singleFmuOutputsToEnvFmuInputs);
                FmuFactory.customFactory = new EnvironmentFMUFactory();
                environmentFMU.createModelDescriptionXML();
            } else {
                throw new InitializationException("Only 1 instance is allowed for single FMU simulation");
            }

        }

        this.startTime = 0d;
        this.endTime = endTime;

        if (connections == null || connections.isEmpty()) {
            throw new Exception("No connections provided");
        }

        try {
            coe.getConfiguration().isStabalizationEnabled = false;
            coe.getConfiguration().global_absolute_tolerance = 0d;
            coe.getConfiguration().global_relative_tolerance = 0d;
            coe.getConfiguration().loggingOn = requestedDebugLoggingCategories != null && !requestedDebugLoggingCategories.isEmpty();
            coe.getConfiguration().visible = false;
            coe.getConfiguration().parallelSimulation = false;
            coe.getConfiguration().simulationProgramDelay = false;
            coe.getConfiguration().hasExternalSignals = false;//TODO maybe?

            this.availableDebugLoggingCategories = coe.initialize(fmus, connections, parameters, stepSizeCalculator, new LogVariablesContainer(new HashMap<>(), outputs));

            // Assert that the logVariables are within the availableDebugLoggingCategories
            boolean logVariablesOK = requestedDebugLoggingCategories.entrySet().stream().allMatch(entry -> {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                return availableDebugLoggingCategories.containsKey(key) &&
                        availableDebugLoggingCategories.get(key).stream().map(logCategory -> logCategory.name).collect(Collectors.toList()).containsAll(value);
            });
            this.requestedDebugLoggingCategories = requestedDebugLoggingCategories;
            if (logVariablesOK == false)
                throw new IllegalArgumentException("Log categories do not align with the log categories within the FMUs");

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

    public void configureSimulationDeltaStepping(Map<String, List<String>> requestedDebugLoggingCategories, boolean reportProgress,
                                                 double liveLogInterval) throws Exception {
        if (simulationHandle == null) {

            Map<ModelConnection.ModelInstance, List<String>> reqDebugLoggingCategories = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : requestedDebugLoggingCategories.entrySet()) {
                reqDebugLoggingCategories.put(ModelConnection.ModelInstance.parse(entry.getKey()), entry.getValue());
            }
            this.simulationHandle = get().getSimulateControlHandle(startTime, endTime, reqDebugLoggingCategories, reportProgress, liveLogInterval);
        }
    }

    public void simulate(double delta) throws Exception {

        if (simulationHandle == null) {
            throw new Exception("Simulation handle not configured");
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

    public void simulate(double delta, List<ModelParameter> inputs) throws Exception {

        if (simulationHandle == null) {
            configureSimulationDeltaStepping(new HashMap<>(), false, 0d);
        }

        simulate(delta);
    }
}
