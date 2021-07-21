package org.intocps.maestro.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.MasterModel;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.MultiModelScenarioVerifier;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.MasterModelMapper;
import org.intocps.maestro.template.ScenarioConfiguration;
import picocli.CommandLine;
import synthesizer.ConfParser.ScenarioConfGenerator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "execute-algorithm", description = "Executes an algorithm generated from a multi-model. If no algorithm is specified " +
        "(as a master model) it will be generated from the multi-model, however this requires the multi-model to include scenario " +
        "verifier information such as reactivity!", mixinStandardHelpOptions = true)
public class ExecuteAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;

    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;

    @CommandLine.Option(names = {"-em", "--extended-multimodel"}, required = true,
            description = "A multi-model or an extended multi-model if no master model is parsed")
    File extendedMultiModelFile;

    @CommandLine.Option(names = {"-al", "--algorithm"},
            description = "A master model (.conf) that contains the algorithm and that is generated from the multi model")
    File algorithmFile;

    @CommandLine.Option(names = {"-ep", "--execution-parameters"}, required = true, description = "Execution parameters (.json)")
    File executionParametersFile;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the outputs will be stored")
    File output;

    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;

    @Override
    public Integer call() throws Exception {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = false;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        String masterModelAsString;
        if (algorithmFile == null) {
            System.out.println("No master model parsed. Generating algorithm from executable model");
            try {
                MultiModelScenarioVerifier multiModel = (new ObjectMapper()).readValue(extendedMultiModelFile, MultiModelScenarioVerifier.class);
                MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);

                masterModelAsString = ScenarioConfGenerator.generate(masterModel, masterModel.name());
                Path algorithmPath = output.toPath().resolve("masterModel.conf");
                Files.write(algorithmPath, masterModelAsString.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.out.println("Unable to generate masterModel: " + e);
                return -1;
            }
        } else {
            masterModelAsString = Files.readString(algorithmFile.toPath());
        }

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode multiModelNode = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(extendedMultiModelFile.getPath()))));
        JsonNode execParamsNode = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(executionParametersFile.getPath()))));
        ScenarioConfiguration scenarioConfiguration =
                getConfigFromMultiModel(multiModelNode, execParamsNode, jsonMapper, util.reporter, masterModelAsString);

        try {
            if (!util.generateSpec(scenarioConfiguration)) {
                return -1;
            }
        } catch (Exception e) {
            System.out.println("Unable to generate specification: " + e);
            return 1;
        }

        if (!util.expand()) {
            return 1;
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!util.typecheck()) {
            return -1;
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return -1;
            }
        }

        util.interpret();
        return 0;
    }

    private ScenarioConfiguration getConfigFromMultiModel(JsonNode multiModelNode, JsonNode execParamsNode, ObjectMapper jsonMapper,
            IErrorReporter errorReporter, String masterModel) throws Exception {

        // Set values from JSON
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = jsonMapper.readValue(jsonMapper.treeAsTokens(multiModelNode.get("fmus")), new TypeReference<>() {
        });
        simulationConfiguration.connections = jsonMapper.readValue(jsonMapper.treeAsTokens(multiModelNode.get("connections")), new TypeReference<>() {
        });
        Map<String, Object> parameters = jsonMapper.readValue(jsonMapper.treeAsTokens(multiModelNode.get("parameters")), new TypeReference<>() {
        });
        Double relTol = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("convergenceRelativeTolerance")), new TypeReference<>() {
        });
        Double absTol = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("convergenceAbsoluteTolerance")), new TypeReference<>() {
        });
        Integer convergenceAttempts = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("convergenceAttempts")), new TypeReference<>() {
        });
        Double startTime = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("startTime")), new TypeReference<>() {
        });
        Double endTime = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("endTime")), new TypeReference<>() {
        });
        Double stepSize = jsonMapper.readValue(jsonMapper.treeAsTokens(execParamsNode.get("stepSize")), new TypeReference<>() {
        });

        // Setup scenarioConfiguration
        return new ScenarioConfiguration(Fmi2SimulationEnvironment.of(simulationConfiguration, errorReporter), masterModel, parameters, relTol,
                absTol, convergenceAttempts, startTime, endTime, stepSize, Pair.of(Framework.FMI2, simulationConfiguration));
    }

    private ScenarioConfiguration getConfigFromExecutableModel(JsonNode executableModel, ObjectMapper jsonMapper, IErrorReporter errorReporter,
            String masterModel) throws Exception {

        // Set values from JSON
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("multiModel").get("fmus")), new TypeReference<>() {
                });
        simulationConfiguration.connections =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("multiModel").get("connections")), new TypeReference<>() {
                });
        Map<String, Object> parameters =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("multiModel").get("parameters")), new TypeReference<>() {
                });
        Double relTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("convergenceRelativeTolerance")),
                new TypeReference<>() {
                });
        Double absTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("convergenceAbsoluteTolerance")),
                new TypeReference<>() {
                });
        Integer convergenceAttempts = jsonMapper
                .readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("convergenceAttempts")), new TypeReference<>() {
                });
        Double startTime =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("startTime")), new TypeReference<>() {
                });
        Double endTime =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("endTime")), new TypeReference<>() {
                });
        Double stepSize =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableModel.get("executionParameters").get("stepSize")), new TypeReference<>() {
                });

        // Setup scenarioConfiguration
        return new ScenarioConfiguration(Fmi2SimulationEnvironment.of(simulationConfiguration, errorReporter), masterModel, parameters, relTol,
                absTol, convergenceAttempts, startTime, endTime, stepSize, Pair.of(Framework.FMI2, simulationConfiguration));
    }
}
