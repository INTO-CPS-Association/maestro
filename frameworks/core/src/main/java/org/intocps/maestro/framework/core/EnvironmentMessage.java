package org.intocps.maestro.framework.core;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentMessage {
    @JsonProperty("end_time")
    public Double endTime = 10.0;
    @JsonProperty("step_size")
    public Double stepSize = 0.1;
    public Map<String, String> fmus;
    public Map<String, List<String>> connections;
    public Map<String, List<String>> logVariables;
    public Map<String, List<String>> livestream;
    public Double liveLogInterval;
    @JsonProperty("algorithm")
    public IAlgorithmConfig algorithm;

    @JsonProperty("logLevels")
    public Map<String, List<String>> logLevels = new HashMap<>();

    @JsonProperty("visible")
    public boolean visible = false;

    @JsonProperty("loggingOn")
    public boolean loggingOn = false;

    @JsonIgnore
    public static String extractInstanceFromKeyInstance(String tuple) {
        String startInstanceSplitSequence = "}.";
        int indexStart = tuple.indexOf(startInstanceSplitSequence);
        return tuple.substring(indexStart + startInstanceSplitSequence.length());
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

    @JsonIgnore
    public Double getLivelogInterval() {
        if (this.liveLogInterval != null) {
            return this.liveLogInterval;
        } else {
            return 0.1;
        }
    }

    @JsonIgnore
    public Map<String, List<String>> getInstanceNameToLogLevels() {
        if (this.logLevels != null) {
            final Map<String, List<String>> ll = new HashMap<>();
            this.logLevels.forEach((k, v) -> ll.put(extractInstanceFromKeyInstance(k), v));
            return ll;
        } else {
            return null;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = FixedStepAlgorithmConfig.class, name = "fixed-step")})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public interface IAlgorithmConfig {
    }

    public static class FixedStepAlgorithmConfig implements IAlgorithmConfig {
        @JsonProperty("size")
        public final Double size;

        @JsonCreator
        public FixedStepAlgorithmConfig(@JsonProperty("size") Double size) {
            this.size = size;
        }

        public Double getSize() {
            return size;
        }
    }
}
