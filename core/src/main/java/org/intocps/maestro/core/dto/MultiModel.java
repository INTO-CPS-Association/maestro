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

    @JsonCreator
    public MultiModel(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
            @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("logVariables") Map<String, List<String>> logVariables,
            @JsonProperty("parallelSimulation") boolean parallelSimulation, @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels) {
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
    }

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

    public Map<String, List<String>> getLogLevels() { return logLevels;}

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
