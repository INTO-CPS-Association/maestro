package org.intocps.maestro.cli;

import cli.VerifyTA;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.ExtendedMultiModel;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.MasterModelMapper;
import org.intocps.maestro.template.ScenarioConfiguration;
import picocli.CommandLine;
import scala.jdk.javaapi.CollectionConverters;
import synthesizer.ConfParser.ScenarioConfGenerator;
import trace_analyzer.TraceAnalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sigver",
        description = "Utilise the scenario verifier tool to generate and verify algorithms. It is also possible to execute scenarios and extended multi-models.",
        mixinStandardHelpOptions = true,
        subcommands = {ExecuteAlgorithmCmd.class, GenerateAlgorithmCmd.class, VisualizeTracesCmd.class, VerifyAlgorithmCmd.class})
public class SigverCmd {
}

@CommandLine.Command(name = "visualize-traces", description = "Visualizes traces for an algorithm that cannot be verified successfully.",
        mixinStandardHelpOptions = true)
class VisualizeTracesCmd implements Callable<Integer> {
    @CommandLine.Parameters(description = "A master model (scenario + algorithm) in .conf format")
    File file;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the visualization files will be stored")
    File output;

    @Override
    public Integer call() throws Exception {
        if (output == null) {
            output = Files.createTempDirectory("tmpDir").toFile();
        }

        if (!VerifyTA.checkEnvironment()) {
            System.out.println("Verification environment is not setup correctly");
            return -1;
        }

        File tempDir = Files.createTempDirectory("tmpDir").toFile();
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(Files.readString(file.toPath()).getBytes()));
        File uppaalFile = Path.of(tempDir.getPath(), "uppaal.xml").toFile();
        File traceFile = Path.of(tempDir.getPath(), "trace.log").toFile();
        try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
            fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
        } catch (Exception e) {
            System.out.println("Unable to write encoded master model to file: " + e);
            return -1;
        }
        // This verifies the algorithm and writes to the trace file.
        int resultCode = VerifyTA.saveTraceToFile(uppaalFile, traceFile);

        // If verification result code is 1 violations of the scenarios' contract were found and a trace has been written to the trace file
        // from which a visualization can be made.
        if (resultCode == 1) {
            Path videoTraceFolder = output.toPath();
            ModelEncoding modelEncoding = new ModelEncoding(masterModel);
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile))) {
                TraceAnalyzer.AnalyseScenario(masterModel.name(), CollectionConverters.asScala(bufferedReader.lines().iterator()), modelEncoding,
                        videoTraceFolder.toString());

            } catch (Exception e) {
                System.out.println("Unable to generate trace visualization: " + e);
                return -1;
            }
            System.out.println("Generated trace visualization in: " + videoTraceFolder.toAbsolutePath());
        }
        // If the verification code is anything else than 1 it is not possible to visualize the trace and this is an "error" for this endpoint
        // even though the verification might have been successful.
        else if (resultCode == 2) {
            System.out.println("Unable to verify algorithm - there is probably a syntax error.");
        } else if (resultCode == 0) {
            System.out.println("Algorithm successfully verified - no traces to visualize.");
        } else {
            System.out.println("Unknown algorithm verification error code encountered: " + resultCode);
            return -1;
        }

        return 0;
    }
}

@CommandLine.Command(name = "verify-algorithm", description = "Verifies an algorithm.", mixinStandardHelpOptions = true)
class VerifyAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Parameters(description = "A master model (scenario + algorithm) in .conf format")
    File masterModelFile;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the encoded master model file will be stored")
    File output;

    public int resultCode;

    @Override
    public Integer call() throws Exception {

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(Files.readString(masterModelFile.toPath()).getBytes()));
        if (output == null) {
            output = Files.createTempDirectory("tmpDir").toFile();
        }
        File uppaalFile = output.toPath().resolve("uppaal.xml").toFile();
        if (VerifyTA.checkEnvironment()) {
            try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
                fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
            } catch (Exception e) {
                System.out.println("Unable to write encoded master model to file: " + e);
            }
            resultCode = VerifyTA.verify(uppaalFile);
        } else {
            System.out.println("Verification environment is not setup correctly");
            return -1;
        }

        System.out.println("Output written to: " + output.getPath());

        if (resultCode == 2) {
            System.out.println("Unable to verify algorithm - there is probably a syntax error.");
        } else if (resultCode == 1) {
            System.out.println("Violations of the scenarios' contract were found - traces can be generated.");
        } else if (resultCode == 0) {
            System.out.println("Algorithm successfully verified - no traces to visualize.");
        } else {
            System.out.println("Unknown algorithm verification error code encountered: " + resultCode);
            return -1;
        }

        return 0;
    }
}

@CommandLine.Command(name = "generate-algorithm", description = "Generates an algorithm from a scenario or multi-model.",
        mixinStandardHelpOptions = true)
class GenerateAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Parameters(description = "A scenario (.conf) or a multi-model (.json)")
    File file;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the algorithm will be stored")
    File output;

    @Override
    public Integer call() throws Exception {
        Path filePath = file.toPath();
        MasterModel masterModel;
        if (FilenameUtils.getExtension(filePath.toString()).equals("conf")) {
            String scenario = Files.readString(filePath);
            masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        } else if (FilenameUtils.getExtension(filePath.toString()).equals("json")) {
            ExtendedMultiModel multiModel = (new ObjectMapper()).readValue(file, ExtendedMultiModel.class);
            masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        } else {
            return -1;
        }

        String algorithm = ScenarioConfGenerator.generate(masterModel, masterModel.name());
        Path algorithmPath = output.toPath().resolve("masterModel.conf");
        Files.write(algorithmPath, algorithm.getBytes(StandardCharsets.UTF_8));

        return 0;
    }
}

@CommandLine.Command(name = "execute-algorithm", description = "Executes an algorithm generated from a multi-model. If no algorithm is passed " +
        "(as a master model) it will be generated from the multi-model, however this requires an extended multi-model that includes scenario " +
        "verifier information such as reactivity!", mixinStandardHelpOptions = true)
class ExecuteAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;

    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;

    @CommandLine.Option(names = {"-mm", "--multi-model"}, required = true,
            description = "A multi-model or an extended multi-model if no master model is passed")
    File extendedMultiModelFile;

    @CommandLine.Option(names = {"-al", "--algorithm"},
            description = "A master model (.conf) that contains the algorithm generated from the multi model")
    File algorithmFile;

    @CommandLine.Option(names = {"-ep", "--execution-parameters"}, required = true, description = "Execution parameters (.json)")
    File executionParametersFile;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the outputs will be stored")
    File output;

    @CommandLine.Option(names = {"-vim", "--verify-mabl"},
            description = "Verify the resulting MaBL spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verifyMabl;

    @CommandLine.Option(names = {"-via", "--verify-algorithm"}, description = "Verify the algorithm. Note this requires UPPAAL to be available!")
    boolean verifyAlgo;

    @Override
    public Integer call() throws Exception {
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = false;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        String masterModelAsString;
        Path algorithmPath;
        if (algorithmFile == null) {
            algorithmPath = output.toPath().resolve("masterModel.conf");
            System.out.println("No master model passed. Generating algorithm from executable model");
            try {
                ExtendedMultiModel multiModel = (new ObjectMapper()).readValue(extendedMultiModelFile, ExtendedMultiModel.class);
                MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);

                masterModelAsString = ScenarioConfGenerator.generate(masterModel, masterModel.name());

                Files.write(algorithmPath, masterModelAsString.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.out.println("Unable to generate masterModel: " + e);
                return -1;
            }
        } else {
            algorithmPath = algorithmFile.toPath();
            masterModelAsString = Files.readString(algorithmPath);
        }

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode multiModelNode = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(extendedMultiModelFile.getPath()))));
        JsonNode execParamsNode = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(executionParametersFile.getPath()))));
        ScenarioConfiguration scenarioConfiguration =
                getConfigFromMultiModel(multiModelNode, execParamsNode, jsonMapper, util.reporter, masterModelAsString);

        if (verifyAlgo) {
            VerifyAlgorithmCmd viaCmd = new VerifyAlgorithmCmd();
            viaCmd.masterModelFile = algorithmPath.toFile();
            viaCmd.output = output;
            viaCmd.call();
            if (viaCmd.resultCode != 0) {
                System.out.println("The algorithm did not verify");
                return -1;
            }
        }

        try {
            if (!util.generateSpec(scenarioConfiguration)) {
                return -1;
            }
        } catch (Exception e) {
            System.out.println("Unable to generate specification: " + e);
            return -1;
        }

        if (!util.expand()) {
            return -1;
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!util.typecheck()) {
            return -1;
        }

        if (verifyMabl != null) {
            if (!util.verify(verifyMabl)) {
                return -1;
            }
        }

        util.interpret();
        return 0;
    }

    private ScenarioConfiguration getConfigFromMultiModel(JsonNode multiModelNode, JsonNode execParamsNode, ObjectMapper jsonMapper,
            IErrorReporter errorReporter, String masterModelAsString) throws Exception {

        if(masterModelAsString.equals("")){
            throw new RuntimeException("Cannot create configuration from empty master model");
        }
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));

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


        if(simulationConfiguration.connections.isEmpty()){
            // Setup connections as defined in the scenario instead of the multi-model (These should be identical)
            List<ConnectionModel> connections = CollectionConverters.asJava(masterModel.scenario().connections());
            Map<String, List<String>> connectionsMap = new HashMap<>();
            connections.forEach(connection -> {
                String[] trgFmuAndInstance = connection.trgPort().fmu().split("_");
                String trgFmuName = trgFmuAndInstance[0];
                String trgInstanceName = trgFmuAndInstance[1];
                String[] srcFmuAndInstance = connection.srcPort().fmu().split("_");
                String srcFmuName = srcFmuAndInstance[0];
                String srcInstanceName = srcFmuAndInstance[1];
                String muModelTrgName = "{" + trgFmuName + "}" + "." + trgInstanceName + "." + connection.trgPort().port();
                String muModelSrcName = "{" + srcFmuName + "}" + "." + srcInstanceName + "." + connection.srcPort().port();
                if (connectionsMap.containsKey(muModelSrcName)) {
                    connectionsMap.get(muModelSrcName).add(muModelTrgName);
                } else {
                    connectionsMap.put(muModelSrcName, new ArrayList<>(Collections.singletonList(muModelTrgName)));
                }
            });

            simulationConfiguration.connections = connectionsMap;
        }

        // Setup scenarioConfiguration
        return new ScenarioConfiguration(Fmi2SimulationEnvironment.of(simulationConfiguration, errorReporter), masterModel, parameters, relTol,
                absTol, convergenceAttempts, startTime, endTime, stepSize, Pair.of(Framework.FMI2, simulationConfiguration));
    }
}