package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "FMUs, connections, parameters, and algorithm options for a co-simulation.")
public class MultiModel {
    @JsonProperty("fmus")
    @Schema(description = "Map from FMU identifiers such as {FMU} to FMU file locations on the server.")
    @JsonPropertyDescription("A map of from fmu names on the form \"{FMU}\" to the path of the .fmu file")
    private final Map<String, String> fmus;
    @JsonProperty("connections")
    @Schema(description = "Output-to-input connections. Keys and values use {FMU}.instanceName.signal; each output can feed multiple inputs.")
    @JsonPropertyDescription("A map of connections where the key and value strings are on the form form \"{FMU}.instanceName.signal\". The mapping is from output to inputs. As a single output can be connected to multiple inputs")
    private final Map<String, List<String>> connections;
    @JsonProperty("parameters")
    @Schema(description = "Parameter values keyed by {FMU}.instanceName.signal, with values matching the FMI scalar type.")
    @JsonPropertyDescription("A map from signal to its value. The key must be on the form \"{FMU}.instanceName.signal\" and value can be any value matching the signal.")
    private final Map<String, Object> parameters;
    @JsonProperty("environmentParameters")
    @Schema(description = "Qualified parameter names whose values should be obtained from the execution environment.")
    @JsonPropertyDescription("A list of parameters which should be obtained from the environment. The strings are on the form form \"{FMU}.instanceName.signal\".")
    private final List<String> environmentParameters;
    @JsonProperty("logVariables")
    @Schema(description = "Signals to record, mapped from {FMU}.instanceName to signal names.")
    @JsonPropertyDescription("A map from fmu instance \"{FMU}.instanceName\" to a list of signal names.")
    private final Map<String, List<String>> logVariables;
    @JsonProperty("parallelSimulation")
    @Schema(description = "Request parallel stepping. The web API currently rejects true.")
    @JsonPropertyDescription("Request parallel stepping. Currently rejected by the web API initialization endpoint when true.")
    private final boolean parallelSimulation;
    @JsonProperty("stabalizationEnabled")
    @Schema(description = "Enable stabilization during initialization. The historical JSON spelling is retained.")
    @JsonPropertyDescription("Enable stabilization generation during initialization.")
    private final boolean stabalizationEnabled;
    @JsonProperty("global_absolute_tolerance")
    @Schema(description = "Global absolute tolerance for initialization and simulation generation.")
    @JsonPropertyDescription("The global absolute tolerance.")
    private final double global_absolute_tolerance;
    @JsonProperty("global_relative_tolerance")
    @Schema(description = "Global relative tolerance for initialization and simulation generation.")
    @JsonPropertyDescription("The global relative tolerance.")
    private final double global_relative_tolerance;
    @JsonProperty("loggingOn")
    @Schema(description = "Enable FMI logging.")
    @JsonPropertyDescription("Enable logging.")
    private final boolean loggingOn;
    @JsonProperty("visible")
    @Schema(description = "Request visible FMU instances during instantiation.")
    @JsonPropertyDescription("The enable visible in FMI.")
    private final boolean visible;
    @JsonProperty("simulationProgramDelay")
    @Schema(description = "Pace simulation steps towards real-time execution.")
    @JsonPropertyDescription("Delay simulation steps. i.e. steps are slowed down to near real time.")
    private final boolean simulationProgramDelay;

    @JsonProperty("overrideLogLevel")
    @Schema(description = "Override the server's global log level.")
    @JsonPropertyDescription("Override global log level of the simulator")
    private final InitializeLogLevel overrideLogLevel;
    @JsonProperty("algorithm")
    @Schema(description = "Step algorithm configuration; type selects fixed-step or var-step.")
    @JsonPropertyDescription("The step algorithm to be used, fixed or variable")
    private final IAlgorithmConfig algorithm;

    @JsonProperty("logLevels")
    @Schema(description = "Enabled FMI log-category names by {FMU}.instanceName.")
    @JsonPropertyDescription("A map of enabled log levels for each FMU. The key is \"{FMU}.instanceName\" and the log levels are the categories specified for each FMU")
    private final Map<String, List<String>> logLevels;

    @JsonProperty("faultInjectConfigurationPath")
    @Schema(description = "Path to the fault-injection configuration on the server.")
    @JsonPropertyDescription("Path to the fault-injection configuration on the server.")
    public String faultInjectConfigurationPath;
    @JsonProperty("faultInjectInstances")
    @Schema(description = "Fault-injection instance configuration, keyed by instance name.")
    @JsonPropertyDescription("Fault-injection instance configuration, keyed by instance name.")
    public Map<String, String> faultInjectInstances;

    @JsonProperty("convergenceAttempts")
    @Schema(description = "Maximum number of convergence attempts used during initialization.")
    @JsonPropertyDescription("Maximum number of convergence attempts used during initialization.")
    private final int convergenceAttempts;

    @JsonProperty("modelTransfers")
    @Schema(description = "Mapping from current instance names to transferred instance names.")
    @JsonPropertyDescription("A mapping from current instance name to the new instance name of that instance being transferred. i.e. A->A if A is transferred as is.")
    public Map<String, String> modelTransfers;

    public static class ModelSwap {
        @JsonProperty("swapInstance")
        @Schema(description = "Name of the replacement instance.")
        @JsonPropertyDescription("The name of the instance swapped in")
        public String swapInstance;
        @JsonProperty("stepCondition")
        @Schema(description = "MaBL condition that permits stepping the replacement instance.")
        @JsonPropertyDescription("The condition that should be true for the step to be allowed. It could be (true)")
        public String stepCondition;
        @JsonProperty("swapCondition")
        @Schema(description = "MaBL condition that triggers replacement of the current instance.")
        @JsonPropertyDescription("The condition that should be true for the model to be swapped in i.e. replacing the previous. The condition could look like (controller.valve ==true) if the instance controller exist and have value as a boolean signal")
        public String swapCondition;
        @JsonProperty("swapConnections")
        @Schema(description = "Connection changes for the replacement instance, from qualified outputs to inputs.")
        @JsonPropertyDescription("A map of new connections like the connections but only with the delta for this new instance. Keys and values are on the form  \"{FMU}.instanceName.signal\". The key is the output and values the inputs")

        public Map<String, List<String>> swapConnections;

        public ModelSwap() {}
    }
    @JsonProperty("modelSwaps")
    @Schema(description = "Model-swap configuration keyed by the instance being replaced.")
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
