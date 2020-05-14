package org.intocps.maestro.webapi.maestrobrokering;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.webapi.controllers.Maestro2SimulationController;

import java.util.List;
import java.util.Map;

public class InitializerUsingCoeConfigCreator {

    public static ParserConfiguration.ParserPluginConfiguration createInitializationJsonNode(
            Maestro2SimulationController.InitializationData legacyInitializationData,
            Maestro2SimulationController.SimulateRequestBody simulateRequestBody) {
        InitializerUsingCoeConfiguration initializerUsingCoeConfiguration = new InitializerUsingCoeConfiguration();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode configuration = mapper.valueToTree(legacyInitializationData);
        initializerUsingCoeConfiguration.configuration = configuration;
        JsonNode start_message = mapper.valueToTree(simulateRequestBody);
        initializerUsingCoeConfiguration.start_message = start_message;

        ParserConfiguration.ParserPluginConfiguration initPluginConfig = new ParserConfiguration.ParserPluginConfiguration();
        ParserConfiguration.ParserPluginIdentification initPluginID = new ParserConfiguration.ParserPluginIdentification();
        initPluginID.name = "InitializerUsingCOE";
        initPluginID.version = "0.0.0";
        initPluginConfig.identification = initPluginID;
        initPluginConfig.config = mapper.valueToTree(initializerUsingCoeConfiguration);
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


