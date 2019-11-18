package org.intocps.orchestration.coe.webapi.services;

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
import java.util.stream.Stream;

public class CoeService {
    private final static Logger logger = LoggerFactory.getLogger(CoeService.class);
    private boolean simulating = false;
    private double startTime = 0d;
    private double endTime = 0d;
    private Map<String, List<ModelDescription.LogCategory>> availableDebugLoggingCategories;
    private Map<String, List<String>> requestedDebugLoggingCategories;
    private Coe coe;
    private Coe.CoeSimulationHandle simulationHandle = null;
    private EnvironmentFMU environmentFMU;

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

    private static List<ModelConnection> createEnvConnection(List<ModelDescription.ScalarVariable> fromVariables,
                                                             ModelConnection.ModelInstance fromInstance, ModelConnection.ModelInstance toInstance) {

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
        // - DONE Create test with fake coe
        // - DONE Create custom FMU factory
        // - Do below stuff if there are any outputs or any inputs


        if (fmus.size() == 1 || (inputs != null && inputs.size() > 0) || (outputs != null && outputs.size() > 0)) {
            // Connections are needed
            if (connections == null) {
                connections = new ArrayList<>();
            }


            // Load the model descriptions for all of the FMUs IFmu fmu = FmuFactory.create(get().getResultRoot(), fmus.entrySet().iterator().next().getValue());
            HashMap<String, List<ModelDescription.ScalarVariable>> modelDescriptions = new HashMap<>();
            for (Map.Entry<String, URI> fmuEntry : fmus.entrySet()) {
                modelDescriptions.put(fmuEntry.getKey(), (new ModelDescription(FmuFactory.create(get().getResultRoot(), fmuEntry.getValue()).getModelDescription())).getScalarVariables());
            }
            //ModelDescription modelDescription = new ModelDescription(fmu.getModelDescription());
            //List<ModelDescription.ScalarVariable> modelDescScalars = modelDescription.getScalarVariables();
            EnvironmentFMU environmentFMU = EnvironmentFMU.CreateEnvironmentFMU("environmentFMU", "environmentInstance");

            fmus.put(environmentFMU.key, new URI("environment://".concat(environmentFMU.fmuName)));

            // Inputs of non-virtual FMUs occur as output of the virtual environment FMU
            if (inputs != null && inputs.size() > 0) {
                // Outputs for the environment FMU with full scalar variable information
                Map<ModelConnection.ModelInstance, List<ModelParameter>> inputsGroupedByInstance = inputs.stream().collect(Collectors.groupingBy(x -> x.variable.instance));
                Map<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envOutputs = inputsGroupedByInstance.entrySet().stream().map(inputEntry -> {
                    List<ModelDescription.ScalarVariable> correspondingScalars = modelDescriptions.get(inputEntry.getKey().key);
                    List<ModelDescription.ScalarVariable> correlatedScalar = inputEntry.getValue().stream().map(inputVar -> correspondingScalars.stream().filter(svMd -> svMd.name.equals(inputVar.variable.variable)).findFirst().get()).collect(Collectors.toList());
                    return new AbstractMap.SimpleEntry<>(inputEntry.getKey(), correlatedScalar);
                }).collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
                environmentFMU.calculateOutputs(envOutputs);

                // Start values for inputs shall be set as values on environment FMU outputs.
                for (ModelParameter input : inputs) {
                    for (Map.Entry<ModelConnection.Variable, ModelDescription.ScalarVariable> entry : environmentFMU.getSourceToEnvironmentVariableOutputs().entrySet()) {
                        if (entry.getKey().toString().equals(input.variable.toString())) {
                            entry.getValue().type.start = input.value;
                            entry.getValue().initial = ModelDescription.Initial.Exact;
                        }

                    }
                }
            }

            // Outputs of non-virtual FMUs occur as inputs of the virtual environment FMU
            if (outputs != null && outputs.size() > 0) {
                // Inputs for the environment FMU with full scalar variable information.
                HashMap<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envInputs = new HashMap<>();
                // Get all the outputs from the configuration.
                // Correlate the outputs with the corresponding model description such that we get the full scalar variable information
                for (Map.Entry<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputEntry : outputs.entrySet()) {
                    List<ModelDescription.ScalarVariable> correlatedOutputs = outputEntry.getValue().stream().map(sv -> {
                        Optional<ModelDescription.ScalarVariable> correlatedScalars = modelDescriptions.get(outputEntry.getKey().key).stream().filter(svMd -> svMd.name.equals(sv.name))
                                .findFirst();
                        return correlatedScalars.get();
                    }).collect(Collectors.toList());
                    envInputs.put(outputEntry.getKey(), correlatedOutputs);
                }

                environmentFMU.calculateInputs(envInputs);
            }


            // Create the connections to and from the environment FMU based on the map from environment FMU
            for (Map.Entry<ModelConnection.Variable, ModelDescription.ScalarVariable> entry :
                    Stream.concat(
                            environmentFMU.getSourceToEnvironmentVariableInputs().entrySet().stream(),
                            environmentFMU.getSourceToEnvironmentVariableOutputs().entrySet().stream())
                            .collect(Collectors.toSet())) {
                ModelConnection.Variable from = null;
                ModelConnection.Variable to = null;
                switch (entry.getValue().causality) {
                    case Parameter:
                    case CalculatedParameter:
                    case Local:
                    case Independent:
                        throw new InitializationException("Environment FMU Scalars are only for inputs and outputs.");
                    case Input:
                        // Env variable is an input
                        from = ModelConnection.Variable.parse(entry.getKey().toString());
                        to = environmentFMU.createVariable(entry.getValue());
                        break;
                    case Output:
                        from = environmentFMU.createVariable(entry.getValue());
                        to = ModelConnection.Variable.parse(entry.getKey().toString());
                        break;
                }
                connections.add(new ModelConnection(from, to));
            }

            FmuFactory.customFactory = new EnvironmentFMUFactory();
            environmentFMU.createModelDescriptionXML();

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

            this.availableDebugLoggingCategories = coe
                    .initialize(fmus, connections, parameters, stepSizeCalculator, new LogVariablesContainer(new HashMap<>(), outputs));

            // Assert that the logVariables are within the availableDebugLoggingCategories
            boolean logVariablesOK = requestedDebugLoggingCategories.entrySet().stream().filter(x -> x.getValue() != null && x.getValue().size() > 0).
                    allMatch(entry -> {
                        String key = entry.getKey();
                        List<String> value = entry.getValue();
                        return availableDebugLoggingCategories.containsKey(key) && availableDebugLoggingCategories.get(key).stream()
                                .map(logCategory -> logCategory.name).collect(Collectors.toList()).containsAll(value);
                    });
            this.requestedDebugLoggingCategories = requestedDebugLoggingCategories;
            if (logVariablesOK == false) {
                throw new IllegalArgumentException("Log categories do not align with the log categories within the FMUs");
            }

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
                                                 double liveLogInterval) throws ModelConnection.InvalidConnectionException {
        if (simulationHandle == null) {

            Map<ModelConnection.ModelInstance, List<String>> reqDebugLoggingCategories = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : requestedDebugLoggingCategories.entrySet()) {
                reqDebugLoggingCategories.put(ModelConnection.ModelInstance.parse(entry.getKey()), entry.getValue());
            }
            this.simulationHandle = get().getSimulateControlHandle(startTime, endTime, reqDebugLoggingCategories, reportProgress, liveLogInterval);
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

    public void simulate(double delta, List<ModelParameter> inputs) throws SimulatorNotConfigured, ModelConnection.InvalidConnectionException {

        if (simulationHandle == null) {
            configureSimulationDeltaStepping(new HashMap<>(), false, 0d);
        }

        // TODO: The inputs to the non-virtual FMUs correspond to the outputs of the environment FMU.
        //  - MISSING TEST
        this.environmentFMU.setOutputValues(inputs);

        //TODO: Insert into state of COE.
        // - MISSING IMPLEMENTATION


        simulate(delta);

        //TODO: Get the inputs from the environment FMU. These correspond to the outputs from the non-virtual FMUs, i.e. the requested outputs.
        // - MISSING TEST
        // - MISSING PROPER RETURN
        environmentFMU.getRequestedOutputValues();
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
