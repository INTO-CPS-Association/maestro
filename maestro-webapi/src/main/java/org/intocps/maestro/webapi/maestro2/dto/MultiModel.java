package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class MultiModel {
    @JsonProperty("fmus")
    final Map<String, String> fmus;
    @JsonProperty("connections")
    final Map<String, List<String>> connections;
    @JsonProperty("parameters")
    final Map<String, Object> parameters;

    public MultiModel(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
            @JsonProperty("parameters") Map<String, Object> parameters){
        this.fmus = fmus;
        this.connections = connections;
        this.parameters = parameters;
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

}
