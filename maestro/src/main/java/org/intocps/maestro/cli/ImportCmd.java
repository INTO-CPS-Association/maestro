package org.intocps.maestro.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "import", description = "Created a specification from various import types. Remember to place all \" +\n" +
        "        \"necessary plugin extensions in the classpath. \n\nHint for sg1 import where menv shoudl be enabled. Use the follding to " +
        "generate the extra input file:'jq '.parameters|keys|{\"environmentParameters\":.}' mm.json > menv.json'", mixinStandardHelpOptions = true)
public class ImportCmd implements Callable<Integer> {
    static final Predicate<File> jsonFileFilter = f -> f.getName().toLowerCase().endsWith(".json");
    static final Predicate<File> mablFileFilter = f -> f.getName().toLowerCase().endsWith(".mabl");
    @CommandLine.Parameters(index = "0", description = "The valid import formats: ${COMPLETION-CANDIDATES}")
    ImportType type;
    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;

    //    @CommandLine.Option(names = {"-el", "--expansion-limit"}, description = "Stop expansion after this amount of loops")
    //    int expansionLimit;
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;
    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;
    @CommandLine.Option(names = {"-if", "--inline-framework-config"}, description = "Inline all framework configs", negatable = true)
    boolean inlineFrameworkConfig;
    @CommandLine.Option(names = {"-fsp", "--fmu-search-path"}, description = "One or more search paths used to resolve relative FMU paths.")
    List<File> fmuSearchPaths;
    @CommandLine.Option(names = {"-i", "--interpret"}, description = "Interpret spec after import")
    boolean interpret;
    @CommandLine.Parameters(index = "1..*", description = "One or more specification files")
    List<File> files;
    @CommandLine.Option(names = "-output", description = "Path to a directory where the imported spec will be stored")
    File output;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private static MaBLTemplateConfiguration generateTemplateSpecificationFromV1(
            MaestroV1SimulationConfiguration simulationConfiguration) throws Exception {
        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder = MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder();

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

        Fmi2SimulationEnvironmentConfiguration environmentConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        environmentConfiguration.fmus = simulationConfiguration.getFmus();
        environmentConfiguration.connections = simulationConfiguration.getConnections();
        environmentConfiguration.faultInjectInstances = simulationConfiguration.getFaultInjectInstances();
        environmentConfiguration.faultInjectConfigurationPath = simulationConfiguration.getFaultInjectConfigurationPath();

        builder.setFrameworkConfig(Framework.FMI2, environmentConfiguration).useInitializer(true, new ObjectMapper().writeValueAsString(initialize))
                .setFramework(Framework.FMI2).setVisible(simulationConfiguration.isVisible()).setLoggingOn(simulationConfiguration.isLoggingOn())
                .setStepAlgorithmConfig(algorithmConfig);

        return builder.build();
    }

    // https://stackoverflow.com/questions/9895041/merging-two-json-documents-using-jackson
    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

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

    @Override
    public Integer call() throws Exception {

        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = inlineFrameworkConfig;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        List<File> mablFiles = Stream.concat(
                files.stream().filter(File::isDirectory).flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles(mablFileFilter::test)))),
                files.stream().filter(File::isFile).filter(mablFileFilter)).collect(Collectors.toList());

        if (!util.parse(mablFiles)) {
            System.err.println("Failed to parse some files");
            return 1;
        }

        List<File> sourceFiles = Stream.concat(
                files.stream().filter(File::isDirectory).flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles(jsonFileFilter::test)))),
                files.stream().filter(File::isFile).filter(jsonFileFilter)).collect(Collectors.toList());


        if (type == ImportType.Sg1) {
            if (!importSg1(util, fmuSearchPaths, sourceFiles)) {
                return 1;
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + type);
        }

        if (!util.expand()) {
            return 1;
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!disableOptimize) {
            util.mabl.optimize();
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!util.typecheck()) {
            return 1;
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return 1;
            }
        }

        if (interpret) {
            util.interpret();
        }
        return 0;
    }

    private boolean importSg1(MablCliUtil util, List<File> fmuSearchPaths, List<File> files) throws Exception {
        if (!files.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JsonNode rootNode = null;
            for (File jsonFile : files) {
                JsonNode tempNode = mapper.readTree(jsonFile);
                rootNode = rootNode == null ? tempNode : merge(rootNode, tempNode);
            }

            MaestroV1SimulationConfiguration config = mapper.treeToValue(rootNode, MaestroV1SimulationConfiguration.class);

            if (fmuSearchPaths != null) {
                for (String key : config.getFmus().keySet()) {
                    String fmuPath = config.getFmus().get(key);
                    if (!new File(fmuPath).exists()) {
                        Path resolved = null;
                        for (File fmuSearchPath : fmuSearchPaths) {
                            resolved = fmuSearchPath.toPath().resolve(fmuPath);

                            if (resolved.toFile().exists()) {
                                break;
                            }

                        }

                        if (resolved != null && resolved.toFile().exists()) {
                            config.getFmus().put(key, resolved.toUri().toString());
                        } else {
                            throw new FileNotFoundException(fmuPath);
                        }
                    }

                }
            }

            MaBLTemplateConfiguration templateConfig = generateTemplateSpecificationFromV1(config);

            util.mabl.generateSpec(templateConfig);
            util.mabl.setRuntimeEnvironmentVariables(config.getParameters());

            return !MablCliUtil.hasErrorAndPrintErrorsAndWarnings(util.verbose, util.reporter);

        } else {
            System.err.println("Missing configuration file for " + spec.name() + ". Please specify a json file.");
            return false;
        }
    }

    enum ImportType {
        Sg1
    }
}
