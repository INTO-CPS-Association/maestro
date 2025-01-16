package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Map;

public class MultiModel {
    @JsonProperty("fmus")
    @JsonPropertyDescription("A map of from fmu names on the form \"{FMU}\" to the path of the .fmu file")
    private final Map<String, String> fmus;
    @JsonProperty("connections")
    @JsonPropertyDescription("A map of connections where the key and value strings are on the form form \"{FMU}.instanceName.signal\". The mapping is from output to inputs. As a single output can be connected to multiple inputs")
    private final Map<String, List<String>> connections;
    @JsonProperty("parameters")
    @JsonPropertyDescription("A map from signal to its value. The key must be on the form \"{FMU}.instanceName.signal\" and value can be any value matching the signal.")
    private final Map<String, Object> parameters;
    @JsonProperty("environmentParameters")
    @JsonPropertyDescription("A list of parameters which should be obtained from the environment. The strings are on the form form \"{FMU}.instanceName.signal\".")
    private final List<String> environmentParameters;
    @JsonProperty("logVariables")
    @JsonPropertyDescription("A map from fmu instance \"{FMU}.instanceName\" to a list of signal names.")
    private final Map<String, List<String>> logVariables;
    @JsonProperty("parallelSimulation")
    @JsonPropertyDescription("If true the dostep part of the algorithm is run in parallel.")
    private final boolean parallelSimulation;
    @JsonProperty("stabalizationEnabled")
    @JsonPropertyDescription("Enable stabilization generation during initialization.")
    private final boolean stabalizationEnabled;
    @JsonProperty("global_absolute_tolerance")
    @JsonPropertyDescription("The global absolute tolerance.")
    private final double global_absolute_tolerance;
    @JsonProperty("global_relative_tolerance")
    @JsonPropertyDescription("The global relative tolerance.")
    private final double global_relative_tolerance;
    @JsonProperty("loggingOn")
    @JsonPropertyDescription("Enable logging.")
    private final boolean loggingOn;
    @JsonProperty("visible")
    @JsonPropertyDescription("The enable visible in FMI.")
    private final boolean visible;
    @JsonProperty("simulationProgramDelay")
    @JsonPropertyDescription("Delay simulation steps. i.e. steps are slowed down to near real time.")
    private final boolean simulationProgramDelay;

    @JsonProperty("overrideLogLevel")
    @JsonPropertyDescription("Override global log level of the simulator")
    private final InitializeLogLevel overrideLogLevel;
    @JsonProperty("algorithm")
    @JsonPropertyDescription("The step algorithm to be used, fixed or variable")
    private final IAlgorithmConfig algorithm;

    @JsonProperty("logLevels")
    @JsonPropertyDescription("A map of enabled log levels for each FMU. The key is \"{FMU}.instanceName\" and the log levels are the categories specified for each FMU")
    private final Map<String, List<String>> logLevels;

    @JsonProperty("faultInjectConfigurationPath")
    public String faultInjectConfigurationPath;
    @JsonProperty("faultInjectInstances")
    public Map<String, String> faultInjectInstances;

    @JsonProperty("convergenceAttempts")
    private final int convergenceAttempts;

    @JsonProperty("modelTransfers")
    @JsonPropertyDescription("A mapping from current instance name to the new instance name of that instance being transferred. i.e. A->A if A is transferred as is.")
    public Map<String, String> modelTransfers;

    public static class ModelSwap {
        @JsonProperty("swapInstance")
        @JsonPropertyDescription("The name of the instance swapped in")
        public String swapInstance;
        @JsonProperty("stepCondition")
        @JsonPropertyDescription("The condition that should be true for the step to be allowed. It could be (true)")
        public String stepCondition;
        @JsonProperty("swapCondition")
        @JsonPropertyDescription("The condition that should be true for the model to be swapped in i.e. replacing the previous. The condition could look like (controller.valve ==true) if the instance controller exist and have value as a boolean signal")
        public String swapCondition;
        @JsonProperty("swapConnections")
        @JsonPropertyDescription("A map of new connections like the connections but only with the delta for this new instance. Keys and values are on the form  \"{FMU}.instanceName.signal\". The key is the output and values the inputs")

        public Map<String, List<String>> swapConnections;

        public ModelSwap() {}
    }
    @JsonProperty("modelSwaps")
    @JsonPropertyDescription("A map of from an instance name to its swap configuration. Thus the key is the instance name in this simulation.")
    public Map<String, ModelSwap> modelSwaps;


    @JsonCreator
    public MultiModel(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
            @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("logVariables") Map<String, List<String>> logVariables,
            @JsonProperty("parallelSimulation") boolean parallelSimulation, @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("faultInjectConfigurationPath") String faultInjectConfigurationPath,
            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances,
            @JsonProperty("convergenceAttempts") int convergenceAttempts,
            @JsonProperty("modelTransfers") Map<String, String> modelTransfers,
            @JsonProperty("modelSwaps") Map<String, ModelSwap> modelSwaps) {
            this.fmus = fmus;
        this.connections = connections;
        this.parameters = parameters;
        this.logVariables = logVariables;
        this.loggingOn = loggingOn;
        this.visible = visible;
        this.simulationProgramDelay = simulationProgramDelay;
        this.parallelSimulation = parallelSimulation;
        this.stabalizationEnabled = stabalizationEnabled;
        this.global_absolute_tolerance = global_absolute_tolerance;
        this.global_relative_tolerance = global_relative_tolerance;
        this.algorithm = algorithm;
        this.overrideLogLevel = overrideLogLevel;
        this.environmentParameters = environmentParameters;
        this.logLevels = logLevels;
        this.faultInjectConfigurationPath = faultInjectConfigurationPath;
        this.faultInjectInstances = faultInjectInstances;
        this.convergenceAttempts = convergenceAttempts;
        this.modelTransfers = modelTransfers;
        this.modelSwaps = modelSwaps;
    }

    public int getConvergenceAttempts() { return convergenceAttempts; }

    public List<String> getEnvironmentParameters() {
        return environmentParameters;
    }

    public InitializeLogLevel getOverrideLogLevel() {
        return overrideLogLevel;
    }

    public Map<String, String> getFmus() {
        return fmus;
    }

    public Map<String, List<String>> getConnections() {
        return connections;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, List<String>> getLogVariables() {
        return logVariables;
    }

    public boolean isParallelSimulation() {
        return parallelSimulation;
    }

    public boolean isStabalizationEnabled() {
        return stabalizationEnabled;
    }

    public double getGlobal_absolute_tolerance() {
        return global_absolute_tolerance;
    }

    public double getGlobal_relative_tolerance() {
        return global_relative_tolerance;
    }

    public boolean isLoggingOn() {
        return loggingOn;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isSimulationProgramDelay() {
        return simulationProgramDelay;
    }

    public Map<String, List<String>> getLogLevels() {
        return logLevels;
    }

    public IAlgorithmConfig getAlgorithm() {
        return algorithm;
    }

    public enum InitializeLogLevel {
        OFF,
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        ALL
    }
}
