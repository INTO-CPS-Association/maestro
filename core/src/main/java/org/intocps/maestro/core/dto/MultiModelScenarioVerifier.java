package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiModelScenarioVerifier extends MultiModel {
    @JsonProperty("scenarioVerifier")
    public final ScenarioVerifier scenarioVerifier;

    @JsonCreator
    public MultiModelScenarioVerifier(@JsonProperty("fmus") Map<String, String> fmus,
            @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("logVariables") Map<String, List<String>> logVariables, @JsonProperty("parallelSimulation") boolean parallelSimulation,
            @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters, @JsonProperty("logLevels") Map<String, List<String>> logLevels,
            @JsonProperty("scenarioVerifier") ScenarioVerifier scenarioVerifier) {
        super(fmus, connections, parameters, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters, logLevels);
        this.scenarioVerifier = scenarioVerifier;
    }

    public static class ScenarioVerifier {
        @JsonProperty("reactivity")
        public final Map<String, Boolean> reactivity;

        @JsonProperty("verification")
        public final boolean verification;

        @JsonProperty("traceVisualization")
        public final boolean traceVisualization;

        public ScenarioVerifier(@JsonProperty("reactivity") Map<String, Boolean> reactivity, @JsonProperty("verification") boolean verification,
                @JsonProperty("visualization") boolean visualization) {
            this.reactivity = reactivity;
            this.verification = verification;
            this.traceVisualization = visualization;
        }
    }
}
