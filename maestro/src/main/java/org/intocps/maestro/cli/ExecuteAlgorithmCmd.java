package org.intocps.maestro.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.template.ScenarioConfiguration;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "executeAlgorithm", description = "Executes an algorithm generated from a scenario or multi-model.",
        mixinStandardHelpOptions = true)
public class ExecuteAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;

    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;

    @CommandLine.Parameters(description = "An executable algorithm as json")
    File executableAlgorihtmAsJson;

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

        // Read JSON
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode executableMM = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(executableAlgorihtmAsJson.getPath()))));

        // Set values from JSON
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("fmus")), new TypeReference<>() {
                });
        simulationConfiguration.connections =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("connections")), new TypeReference<>() {
                });
        String masterModel = executableMM.get("masterModel").textValue();
        Map<String, Object> parameters =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("parameters")), new TypeReference<>() {
                });
        Double relTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceRelativeTolerance")),
                new TypeReference<>() {
                });
        Double absTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceAbsoluteTolerance")),
                new TypeReference<>() {
                });
        Integer convergenceAttempts = jsonMapper
                .readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceAttempts")), new TypeReference<>() {
                });
        Double startTime =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("startTime")), new TypeReference<>() {
                });
        Double endTime = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("endTime")), new TypeReference<>() {
        });
        Double stepSize =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("stepSize")), new TypeReference<>() {
                });

        // Setup scenarioConfiguration
        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, util.reporter);
        ScenarioConfiguration scenarioConfiguration =
                new ScenarioConfiguration(simulationEnvironment, masterModel, parameters, relTol, absTol, convergenceAttempts, startTime, endTime,
                        stepSize, Pair.of(Framework.FMI2, simulationConfiguration));
        try{
            if (!util.generateSpec(scenarioConfiguration)) {
                return -1;
            }
        }
        catch (Exception e){
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
}
