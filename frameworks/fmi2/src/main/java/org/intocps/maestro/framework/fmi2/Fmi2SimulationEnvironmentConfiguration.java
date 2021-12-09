package org.intocps.maestro.framework.fmi2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fmi2SimulationEnvironmentConfiguration {
    private final Map<String, String> _fmus;
    private final Map<String, List<String>> _connections;
    @Deprecated
    public Map<String, List<String>> logVariables;
    @Deprecated
    public Map<String, List<String>> livestream;
    public Map<String, List<String>> variablesToLog;
    public String faultInjectConfigurationPath;
    public Map<String, String> faultInjectInstances;

    public static Fmi2SimulationEnvironmentConfiguration createFromJsonNode(JsonNode node) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if(!node.has("fmus")){
            // Throw
        }
        if(!node.has("connections")) {
            // Throw
        }
        Map<String, String> fmus = mapper.readValue(mapper.treeAsTokens(node.get("fmus")), new TypeReference<>() {
        });
        Map<String, List<String>> connections = mapper.readValue(mapper.treeAsTokens(node.get("connections")), new TypeReference<>() {
        });
        Fmi2SimulationEnvironmentConfiguration configuration = new Fmi2SimulationEnvironmentConfiguration(connections, fmus);

        if(node.has("faultInjectConfigurationPath")){
            configuration.faultInjectConfigurationPath = mapper.readValue(mapper.treeAsTokens(node.get("faultInjectConfigurationPath")), new TypeReference<>() {
            });
        }
        if(node.has("faultInjectInstances")) {
            configuration.faultInjectInstances = mapper.readValue(mapper.treeAsTokens(node.get("faultInjectInstances")), new TypeReference<>() {
            });
        }
        if(node.has("variablesToLog")) {
            configuration.variablesToLog = mapper.readValue(mapper.treeAsTokens(node.get("variablesToLog")), new TypeReference<>() {
            });
        }
        if(node.has("livestream")) {
            configuration.livestream = mapper.readValue(mapper.treeAsTokens(node.get("livestream")), new TypeReference<>() {
            });
        }
        if(node.has("logVariables")) {
            configuration.logVariables = mapper.readValue(mapper.treeAsTokens(node.get("logVariables")), new TypeReference<>() {
            });
        }
        return configuration;
    }

    public Fmi2SimulationEnvironmentConfiguration(Map<String, List<String>> connections, Map<String, String> fmus) {
        if(connections == null || connections.size() < 1) {
            // Throw
        }
        if(fmus == null || fmus.size() < 1) {
            // Throw
        }
        _fmus = fmus;
        _connections = connections;
    }

    public Map<String, String> getFmus()  {
        return _fmus;
    }

    public Map<String, List<String>> getConnections() {
        return _connections;
    }

    @JsonIgnore
    public Map<String, URI> getFmuFiles() throws Exception {
        Map<String, URI> files = new HashMap<>();
        if (getFmus() != null) {
            for (Map.Entry<String, String> entry : getFmus().entrySet()) {
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
