package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitializationData {

    @JsonIgnore
    @JsonProperty("liveGraphColumns")
    final Object liveGraphColumns = null;

    @JsonIgnore
    @JsonProperty("liveGraphVisibleRowCount")
    final Object liveGraphVisibleRowCount = null;

    @JsonIgnore
    @JsonProperty("livestreamInterval")
    final Object livestreamInterval = null;

    @JsonProperty("fmus")
    final Map<String, String> fmus;
    @JsonProperty("connections")
    final Map<String, List<String>> connections;
    @JsonProperty("parameters")
    final Map<String, Object> parameters;

    @JsonProperty("environmentParameters")
    final List<String> environmentParameters;

    @JsonProperty("livestream")
    final Map<String, List<String>> livestream;
    @JsonProperty("logVariables")
    final Map<String, List<String>> logVariables;
    @JsonProperty("parallelSimulation")
    final boolean parallelSimulation;
    @JsonProperty("stabalizationEnabled")
    final boolean stabalizationEnabled;
    @JsonProperty("global_absolute_tolerance")
    final double global_absolute_tolerance;
    @JsonProperty("global_relative_tolerance")
    final double global_relative_tolerance;
    @JsonProperty("loggingOn")
    final boolean loggingOn;
    @JsonProperty("visible")
    final boolean visible;
    @JsonProperty("simulationProgramDelay")
    final boolean simulationProgramDelay;
    @JsonProperty("hasExternalSignals")
    final boolean hasExternalSignals;
    @JsonProperty("overrideLogLevel")
    final InitializeLogLevel overrideLogLevel;
    @JsonProperty("algorithm")
    IAlgorithmConfig algorithm;

    @JsonCreator
    public InitializationData(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
            @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("livestream") Map<String, List<String>> livestream,
            @JsonProperty("logVariables") Map<String, List<String>> logVariables, @JsonProperty("parallelSimulation") boolean parallelSimulation,
            @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("hasExternalSignals") boolean hasExternalSignals, @JsonProperty("algorithm") IAlgorithmConfig algorithm,
            @JsonProperty("overrideLogLevel") final InitializeLogLevel overrideLogLevel,
            @JsonProperty("liveGraphColumns") final Object liveGraphColumns,
            @JsonProperty("liveGraphVisibleRowCount") final Object liveGraphVisibleRowCount,
            @JsonProperty("livestreamInterval") final Object livestreamInterval,
            @JsonProperty("environmentParameters") final List<String> environmentParameters) {
        this.fmus = fmus;
        this.connections = connections;
        this.parameters = parameters;
        this.livestream = livestream;
        this.logVariables = logVariables;
        this.loggingOn = loggingOn;
        this.visible = visible;
        this.simulationProgramDelay = simulationProgramDelay;
        this.hasExternalSignals = hasExternalSignals;
        this.parallelSimulation = parallelSimulation;
        this.stabalizationEnabled = stabalizationEnabled;
        this.global_absolute_tolerance = global_absolute_tolerance;
        this.global_relative_tolerance = global_relative_tolerance;
        this.algorithm = algorithm;
        this.overrideLogLevel = overrideLogLevel;
        this.environmentParameters = environmentParameters;
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

    public Map<String, List<String>> getLivestream() {
        return livestream;
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

    public boolean isHasExternalSignals() {
        return hasExternalSignals;
    }

    public IAlgorithmConfig getAlgorithm() {
        return algorithm;
    }

    @JsonIgnore
    public Map<String, URI> getFmuFiles() throws Exception {
        Map<String, URI> files = new HashMap<>();

        if (fmus != null) {
            for (Map.Entry<String, String> entry : fmus.entrySet()) {
                try {
                    files.put(entry.getKey(), new URI(entry.getValue()));
                } catch (Exception e) {
                    throw new Exception(entry.getKey() + "-" + entry.getValue() + ": " + e.getMessage(), e);
                }
            }
        }

        return files;
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
