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
import org.intocps.maestro.core.Framework;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PluginFactory {

    final static ObjectMapper mapper = new ObjectMapper();

    public static <T extends IMaestroPlugin> Collection<T> getPlugins(Class<T> type) {
        return getPlugins(type, null);
    }

    public static <T extends IMaestroPlugin> Collection<T> getPlugins(Class<T> type, Framework framework) {
        Reflections reflections = new Reflections("org.intocps.maestro", new SubTypesScanner());

        try {

            Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);

            Predicate<? super Class<? extends T>> frameworkFilter = (Predicate<Class<? extends T>>) aClass -> framework == null || aClass
                    .isAnnotationPresent(SimulationFramework.class) && (aClass.getAnnotation(SimulationFramework.class)
                    .framework() == framework || aClass.getAnnotation(SimulationFramework.class).framework() == Framework.Any);

            return subTypes.stream().filter(frameworkFilter).map(c -> {
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
        if (io == null) {
            return new HashMap<>();
        }

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
