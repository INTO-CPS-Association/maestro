package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.plugin.PluginFactory;
import org.intocps.maestro.plugin.PluginIdentification;

import java.util.List;
import java.util.Map;

public class InitializerUsingCoeConfigCreator {

    public static PluginFactory.PluginConfiguration createInitializationJsonNode(
            Maestro2SimulationController.InitializationData legacyInitializationData,
            Maestro2SimulationController.SimulateRequestBody simulateRequestBody) throws JsonProcessingException {
        InitializerUsingCoeConfiguration initializerUsingCoeConfiguration = new InitializerUsingCoeConfiguration();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode configuration = mapper.valueToTree(legacyInitializationData);
        initializerUsingCoeConfiguration.configuration = configuration;
        System.out.println(configuration.toString());
        JsonNode start_message = mapper.valueToTree(simulateRequestBody);
        initializerUsingCoeConfiguration.start_message = start_message;

        PluginFactory.PluginConfiguration initPluginConfig = new PluginFactory.PluginConfiguration();
        PluginIdentification initPluginID = new PluginIdentification();
        initPluginID.name = "InitializerUsingCOE";
        initPluginID.version = "0.0.0";
        initPluginConfig.identification = initPluginID;
        initPluginConfig.config = mapper.valueToTree(initializerUsingCoeConfiguration).toString();
        return initPluginConfig;
    }

    public static class InitializerUsingCoeConfiguration {
        @JsonProperty
        JsonNode configuration;
        @JsonProperty
        JsonNode start_message;
    }

    public class MaestroInitializationJsonNode {
        public Map<String, String> fmus;
        public Map<String, List<String>> connections;
        public Map<String, Object> parameters;
        public Map<String, Object> algorithm;
    }
}


