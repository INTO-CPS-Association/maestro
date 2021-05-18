package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class MultiModelScenarioVerifier extends MultiModel{
    @JsonProperty("reactivity")
    final Map<String, Boolean> reactivity;

    public MultiModelScenarioVerifier(@JsonProperty("fmus") Map<String, String> fmus,
            @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("reactivity") Map<String, Boolean> reactivity) {
        super(fmus, connections, parameters);
        this.reactivity = reactivity;
    }

    public Map<String, Boolean> getReactivity() {
        return this.reactivity;
    }
}
