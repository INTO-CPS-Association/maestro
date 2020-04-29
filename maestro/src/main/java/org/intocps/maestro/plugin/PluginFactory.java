package org.intocps.maestro.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class PluginFactory {

    final static ObjectMapper mapper = new ObjectMapper();

    public static Collection<IMaestroPlugin> getPlugins() {
        Reflections reflections = new Reflections(IMaestroPlugin.class.getPackage().getName(), new SubTypesScanner());

        try {

            Set<Class<? extends IMaestroPlugin>> subTypes = reflections.getSubTypesOf(IMaestroPlugin.class);

            return subTypes.stream().map(c -> {
                try {
                    return c.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        } catch (ReflectionsException e) {
            if (e.getMessage().contains("not configured")) {
                return Collections.emptySet();
            }
            throw e;
        }
    }

    public static Map<String, String> parsePluginConfiguration(InputStream io) throws IOException {
        List<PluginConfiguration> configs = mapper.readValue(io, new TypeReference<List<PluginConfiguration>>() {
        });
        if (configs != null) {
            return configs.stream().collect(Collectors.toMap(c -> c.identification.name + "-" + c.identification.version, c -> c.config));
        }
        return null;
    }

    public static Map<String, String> parsePluginConfiguration(File contextFile) throws IOException {
        if (contextFile == null) {
            return new HashMap<>();
        }
        try (InputStream io = new FileInputStream(contextFile)) {

            return parsePluginConfiguration(io);
        }
    }

    private static class PluginConfiguration {
        @JsonDeserialize(using = RawJsonDeserializer.class)
        private String config;
        @JsonProperty("identification")
        private PluginIdentification identification;
    }

    public static class RawJsonDeserializer extends JsonDeserializer<String> {


        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            JsonNode node = mapper.readTree(jp);
            return mapper.writeValueAsString(node);
        }
    }
}
