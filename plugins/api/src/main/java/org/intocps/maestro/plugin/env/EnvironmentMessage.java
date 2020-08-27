package org.intocps.maestro.plugin.env;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentMessage {
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
