package org.intocps.maestro.cli;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.core.dto.MultiModel;
import org.intocps.maestro.framework.core.EnvironmentException;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.intocps.maestro.template.MaBLTemplateConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MaestroV1SimulationConfiguration extends MultiModel {

    @JsonProperty("startTime")
    @JsonPropertyDescription("The start time of the simulation. Usually 0.")
    private final double startTime;
    @JsonProperty("endTime")
    @JsonPropertyDescription("The duration of the simulation in seconds. This is used by the fmus to allocate memory.")
    private final Double endTime;
    @JsonProperty("reportProgress")
    @JsonPropertyDescription("If true the simulation will attempt to report progress.")
    private final Boolean reportProgress;


    public Map<String, List<String>> getLiveStreamVariables() {
        return liveStreamVariables;
    }



    @JsonProperty("livestream")
    @JsonPropertyDescription("A map from fmu instance \"{FMU}.instanceName\" to a list of signal names that should be live streamed during the simulation.")
    private final Map<String, List<String>> liveStreamVariables;

    @JsonCreator
    public MaestroV1SimulationConfiguration(@JsonProperty("fmus") Map<String, String> fmus,
                                            @JsonProperty("connections") Map<String, List<String>> connections,
                                            @JsonProperty("parameters") Map<String, Object> parameters,
                                            @JsonProperty("logVariables") Map<String, List<String>> logVariables,
                                            @JsonProperty("parallelSimulation") boolean parallelSimulation,
                                            @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
                                            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
                                            @JsonProperty("global_relative_tolerance") double global_relative_tolerance,
                                            @JsonProperty("loggingOn") boolean loggingOn,
                                            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
                                            @JsonProperty("algorithm") IAlgorithmConfig algorithm,
                                            @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
                                            @JsonProperty("environmentParameters") List<String> environmentParameters,
                                            @JsonProperty("logLevels") Map<String, List<String>> logLevels,
                                            @JsonProperty("startTime") double startTime, @JsonProperty("endTime") Double endTime,
                                            @JsonProperty("reportProgress") Boolean reportProgress,
                                            @JsonProperty("faultInjectConfigurationPath") String faultInjectConfigurationPath,
                                            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances,
                                            @JsonProperty("convergenceAttempts") int convergenceAttempts,
                                            @JsonProperty("modelTransfers") Map<String, String> modelTransfers,
                                            @JsonProperty("modelSwaps") Map<String, ModelSwap> modelSwaps,@JsonProperty("livestream") Map<String, List<String>> livestreamVariables) {
        super(fmus, connections, parameters,logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters, logLevels,
                faultInjectConfigurationPath, faultInjectInstances, convergenceAttempts, modelTransfers, modelSwaps);
        this.startTime = startTime;
        this.endTime = endTime;
        this.reportProgress = reportProgress;
        this.liveStreamVariables = livestreamVariables;
    }

    public double getStartTime() {
        return startTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public Boolean getReportProgress() {
        return reportProgress;
    }

    public String getFaultInjectConfigurationPath() {
        return faultInjectConfigurationPath;
    }

    public Map<String, String> getFaultInjectInstances() {
        return faultInjectInstances;
    }

    public Map<String, String> getModelTransfers() {
        return modelTransfers;
    }

    public Map<String, ModelSwap> getModelSwaps() {
        return modelSwaps;
    }

    @JsonIgnore
    public MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder configure(
            MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder) throws Exception {
        MaestroV1SimulationConfiguration simulationConfiguration = this;
        if (simulationConfiguration.getLogLevels() != null) {
            // Loglevels from app consists of {key}.instance: [loglevel1, loglevel2,...] but have to be: instance: [loglevel1, loglevel2,...].
            Map<String, List<String>> removedFMUKeyFromLogLevels = simulationConfiguration.getLogLevels().entrySet().stream().collect(
                    Collectors.toMap(
                            entry -> MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getFmuInstanceFromFmuKeyInstance(entry.getKey()),
                            Map.Entry::getValue));
            builder.setLogLevels(removedFMUKeyFromLogLevels);
        }

        Map<String, Object> initialize = new HashMap<>();
        initialize.put("parameters", simulationConfiguration.getParameters());
        initialize.put("environmentParameters", simulationConfiguration.getEnvironmentParameters());

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = simulationConfiguration.getStartTime();
        algorithmConfig.endTime = simulationConfiguration.getEndTime();
        algorithmConfig.stepAlgorithm = simulationConfiguration.getAlgorithm();
        algorithmConfig.absoluteTolerance = simulationConfiguration.getGlobal_absolute_tolerance();
        algorithmConfig.relativeTolerance = simulationConfiguration.getGlobal_relative_tolerance();
        algorithmConfig.simulationProgramDelay = simulationConfiguration.isSimulationProgramDelay();
        algorithmConfig.stabilisation = simulationConfiguration.isStabalizationEnabled();

        Fmi2SimulationEnvironmentConfiguration environmentConfiguration =
                new Fmi2SimulationEnvironmentConfiguration(simulationConfiguration.getConnections(), simulationConfiguration.getFmus());
        environmentConfiguration.faultInjectInstances = simulationConfiguration.getFaultInjectInstances();
        environmentConfiguration.faultInjectConfigurationPath = simulationConfiguration.getFaultInjectConfigurationPath();
        environmentConfiguration.logVariables = simulationConfiguration.getLogVariables();
        environmentConfiguration.livestream = simulationConfiguration.getLiveStreamVariables();
        environmentConfiguration.modelTransfers = simulationConfiguration.getModelTransfers();
        environmentConfiguration.modelSwaps = simulationConfiguration.getModelSwaps();

        builder.setFrameworkConfig(Framework.FMI2, environmentConfiguration).useInitializer(true, new ObjectMapper().writeValueAsString(initialize))
                .setFramework(Framework.FMI2).setVisible(simulationConfiguration.isVisible()).setLoggingOn(simulationConfiguration.isLoggingOn())
                .setStepAlgorithmConfig(algorithmConfig);
        return builder;
    }


    @JsonIgnore
    public static MaestroV1SimulationConfiguration parse(File... files) throws IOException {
        return parse(Arrays.asList(files));
    }

    @JsonIgnore
    public static MaestroV1SimulationConfiguration parse(List<File> files) throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonNode rootNode = null;
        for (File jsonFile : files) {
            if (jsonFile != null && jsonFile.exists()) {
                JsonNode tempNode = mapper.readTree(jsonFile);
                rootNode = rootNode == null ? tempNode : merge(rootNode, tempNode);
            }
        }

        return mapper.treeToValue(rootNode, MaestroV1SimulationConfiguration.class);
    }

    // https://stackoverflow.com/questions/9895041/merging-two-json-documents-using-jackson
    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() && updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }

    public static class JsonSchemaGenerator {
        private static SchemaGenerator generator = null;

        public static void generate(Class clz, Path location) throws IOException {
            SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                    SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
            configBuilder.with(new JacksonModule());
            configBuilder.with(new JakartaValidationModule());

            configBuilder.with(new CustomMapTypeResolver(() -> generator));

            generator = new SchemaGenerator(configBuilder.build());
            String schema = generator.generateSchema(clz).toPrettyString();


            var schemaFile = location.resolve(clz.getSimpleName() + ".json").toFile();
            if (schemaFile.getParentFile() != null) {
                schemaFile.getParentFile().mkdirs();
            }
            FileUtils.write(schemaFile, schema, "UTF-8");
        }


        public static class CustomMapTypeResolver implements CustomDefinitionProvider {

            final Supplier<SchemaGenerator> generator;

            public CustomMapTypeResolver(Supplier<SchemaGenerator> generator) {
                this.generator = generator;
            }

            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, TypeContext context) {
                if (javaType.isInstanceOf(Map.class) && javaType.getTypeBindings().size() == 2 && javaType.getTypeBindings().getBoundType(0)
                        .isInstanceOf(String.class)) {
                    ObjectNode customSchema = JsonNodeFactory.instance.objectNode();
                    customSchema.put("type", "object");
                    ObjectNode additionalProperties = JsonNodeFactory.instance.objectNode();
                    ResolvedType mapTarget = javaType.getTypeBindings().getBoundType(1);
                    if (mapTarget.isInstanceOf(String.class)) {
                        additionalProperties.put("type", "string");
                        customSchema.set("additionalProperties", additionalProperties);
                        return new CustomDefinition(customSchema);
                    } else if (mapTarget.isInstanceOf(List.class)) {
                        additionalProperties.put("type", "array");
                        additionalProperties.set("items", JsonNodeFactory.instance.objectNode().put("type", "string"));
                        customSchema.set("additionalProperties", additionalProperties);
                        return new CustomDefinition(customSchema);
                    } else if (mapTarget.isInstanceOf(MultiModel.ModelSwap.class)) {
                        customSchema.set("additionalProperties", generator.get().generateSchema(mapTarget));
                        return new CustomDefinition(customSchema);
                    }
                }
                return null;
            }
        }


    }
}
