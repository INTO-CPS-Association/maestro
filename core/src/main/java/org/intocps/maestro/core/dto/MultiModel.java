package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class MultiModel {
    @JsonProperty("fmus")
    private final Map<String, String> fmus;
    @JsonProperty("connections")
    private final Map<String, List<String>> connections;
    @JsonProperty("parameters")
    private final Map<String, Object> parameters;
    @JsonProperty("environmentParameters")
    private final List<String> environmentParameters;
    @JsonProperty("logVariables")
    private final Map<String, List<String>> logVariables;
    @JsonProperty("parallelSimulation")
    private final boolean parallelSimulation;
    @JsonProperty("stabalizationEnabled")
    private final boolean stabalizationEnabled;
    @JsonProperty("global_absolute_tolerance")
    private final double global_absolute_tolerance;
    @JsonProperty("global_relative_tolerance")
    private final double global_relative_tolerance;
    @JsonProperty("loggingOn")
    private final boolean loggingOn;
    @JsonProperty("visible")
    private final boolean visible;
    @JsonProperty("simulationProgramDelay")
    private final boolean simulationProgramDelay;

    @JsonProperty("overrideLogLevel")
    private final InitializeLogLevel overrideLogLevel;
    @JsonProperty("algorithm")
    private final IAlgorithmConfig algorithm;

    @JsonProperty("logLevels")
    private final Map<String, List<String>> logLevels;

    @JsonProperty("faultInjectConfigurationPath")
    public String faultInjectConfigurationPath;
    @JsonProperty("faultInjectInstances")
    public Map<String, String> faultInjectInstances;

    @JsonProperty("convergenceAttempts")
    private final int convergenceAttempts;

    @JsonProperty("modelTransfers")
    public Map<String, String> modelTransfers;

    public static class ModelSwap {
        @JsonProperty("swapInstance")
        public String swapInstance;
        @JsonProperty("stepCondition")
        public String stepCondition;
        @JsonProperty("swapCondition")
        public String swapCondition;
        @JsonProperty("swapConnections")
        public Map<String, List<String>> swapConnections;

        public ModelSwap() {}
    }
    @JsonProperty("modelSwaps")
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
