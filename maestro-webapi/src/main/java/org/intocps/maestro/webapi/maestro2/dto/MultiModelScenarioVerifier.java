package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class MultiModelScenarioVerifier extends MultiModel{
    @JsonProperty("scenarioVerifier")
    public final ScenarioVerifier scenarioVerifier;

    public MultiModelScenarioVerifier(@JsonProperty("fmus") Map<String, String> fmus,
            @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("scenarioVerifier") ScenarioVerifier scenarioVerifier) {
        super(fmus, connections, parameters);
        this.scenarioVerifier = scenarioVerifier;
    }

    public static class ScenarioVerifier {
        @JsonProperty("reactivity")
        public final Map<String, Boolean> reactivity;

        @JsonProperty("verification")
        public final boolean verification;

        @JsonProperty("visualization")
        public final boolean visualization;

        public ScenarioVerifier(@JsonProperty("reactivity") Map<String, Boolean> reactivity, @JsonProperty("verification") boolean verification,
                @JsonProperty("visualization") boolean visualization) {
            this.reactivity = reactivity;
            this.verification = verification;
            this.visualization = visualization;
        }
    }
}
