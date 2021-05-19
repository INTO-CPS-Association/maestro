package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class MultiModelScenarioVerifier extends MultiModel{
    @JsonProperty("ScenarioVerifier")
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

        @JsonProperty("uppaal-verification")
        public final boolean verify;


        public ScenarioVerifier(@JsonProperty("reactivity") Map<String, Boolean> reactivity, @JsonProperty("uppaal-verification") boolean verify) {
            this.reactivity = reactivity;
            this.verify = verify;
        }
    }
}
