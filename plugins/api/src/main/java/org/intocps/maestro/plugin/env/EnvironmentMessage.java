package org.intocps.maestro.plugin.env;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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

}
