package org.intocps.maestro.framework.fmi2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.intocps.maestro.core.dto.MultiModel;
import org.intocps.maestro.framework.core.EnvironmentException;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fmi2SimulationEnvironmentConfiguration {
    private final Map<String, String> fmus;
    private final Map<String, List<String>> connections;
    @Deprecated
    public Map<String, List<String>> logVariables;
    @Deprecated
    public Map<String, List<String>> livestream;
    public Map<String, List<String>> variablesToLog;
    public String faultInjectConfigurationPath;
    public Map<String, String> faultInjectInstances;
    public Map<String, String> modelTransfers;
    public Map<String, MultiModel.ModelSwap> modelSwaps;

    public static Fmi2SimulationEnvironmentConfiguration createFromJsonString(String jsonData) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonData);
        if (!node.has("fmus")) {
            throw new EnvironmentException("Cannot generate simulation environment configuration without FMUs");
        }
        if (!node.has("connections")) {
            throw new EnvironmentException("Cannot generate simulation environment configuration without any connections");
        }
        Map<String, String> fmus = mapper.readValue(mapper.treeAsTokens(node.get("fmus")), new TypeReference<>() {
        });
        Map<String, List<String>> connections = mapper.readValue(mapper.treeAsTokens(node.get("connections")), new TypeReference<>() {
        });
        ((ObjectNode) node).remove("fmus");
        ((ObjectNode) node).remove("connections");
        return mapper.readerForUpdating(new Fmi2SimulationEnvironmentConfiguration(connections, fmus)).readValue(node);
    }

    public Fmi2SimulationEnvironmentConfiguration(Map<String, List<String>> connections, Map<String, String> fmus) throws EnvironmentException {
        if (connections == null || connections.size() < 1) {
            throw new EnvironmentException("Cannot generate simulation environment configuration without any connections");
        }
        if (fmus == null || fmus.size() < 1) {
            throw new EnvironmentException("Cannot generate simulation environment configuration without FMUs");
        }
        validateFmusInConnectionsMatchesFmus(connections, fmus);
        this.fmus = fmus;
        this.connections = connections;
    }

    private void validateFmusInConnectionsMatchesFmus(Map<String, List<String>> connections, Map<String, String> fmus) throws EnvironmentException {
        // The following format is expected: {FMU_NAME}.INSTANCE_NAME.PORT_NAME
        List<String> fmuNames = fmus.keySet().stream().map(key -> key.replaceAll("[{}]", "").strip()).collect(Collectors.toList());
        List<String> fmuNamesInConnections = connections.entrySet().stream().flatMap(entry ->
            Stream.concat(Stream.of(entry.getKey()), entry.getValue().stream()).map(connection -> connection.split("}")[0].replace("{", "").strip())
        ).collect(Collectors.toList());

        for (String fmuNameInConnection : fmuNamesInConnections) {
            if (!fmuNames.contains(fmuNameInConnection)) {
                throw new EnvironmentException("FMU with name '" + fmuNameInConnection + "' from connections cannot be located in fmus");
            }
        }
    }

    public Map<String, String> getFmus() {
        return fmus;
    }

    public Map<String, List<String>> getConnections() {
        return connections;
    }

    public Map<String, List<String>> getModelSwapConnections() {
        Map<String, List<String>> connections = new HashMap<>();
        for (Map.Entry<String, MultiModel.ModelSwap> entry : modelSwaps.entrySet()) {
            MultiModel.ModelSwap swap = entry.getValue();
            if (swap.swapConnections != null) {
                connections.putAll(swap.swapConnections);
            }
        }
        return connections;
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
