package org.intocps.maestro.framework.fmi2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fmi2SimulationEnvironmentConfiguration {
    public Map<String, String> fmus;
    public Map<String, List<String>> connections;
    @Deprecated
    public Map<String, List<String>> logVariables;
    @Deprecated
    public Map<String, List<String>> livestream;

    public Map<String, List<String>> variablesToLog;

    @JsonProperty("faultInjectConfigurationPath")
    public String faultInjectConfigurationPath;
    @JsonProperty("faultInjectInstances")
    public Map<String, String> faultInjectInstances;

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
                    // This fix is related to removing an erroneous leading / in the URI.
                    // See https://github.com/INTO-CPS-Association/into-cps-application/issues/136
                    URI uri = URI.create(entry.getValue());
                    if (uri.getScheme() == null || uri.getScheme().equals("file")) {
                        if (!uri.isAbsolute()) {
                            uri = new File(".").toURI().resolve(uri);
                        }
                        File f = new File(uri);
                        uri = f.toURI();
                    }
                    files.put(entry.getKey(), uri);

                } catch (Exception e) {
                    throw new Exception(entry.getKey() + "-" + entry.getValue() + ": " + e.getMessage(), e);
                }
            }
        }


        return files;
    }
}
