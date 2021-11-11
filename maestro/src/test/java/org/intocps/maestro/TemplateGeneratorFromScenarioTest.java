package org.intocps.maestro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.MasterModel;
import core.ScenarioLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.intocps.maestro.FullSpecTest.getWorkingDirectory;
import static org.intocps.maestro.JacobianStepBuilderTest.csvCompare;

public class TemplateGeneratorFromScenarioTest {
    /**
     * Data Provider
     *
     * @return
     */
    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "template_generator_from_scenario").toFile().listFiles()))
                .filter(n -> !n.getName().equals("stabilization")).map(f -> Arguments.arguments(f.getName(), f));
        //TODO: remove filter when stabilization scenario works
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void generateMaBLSpecFromExecutableScenario(String name, File directory) throws Exception {

        // ARRANGE
        File workingDirectory = getWorkingDirectory(directory, this.getClass());
        IErrorReporter errorReporter = new ErrorReporter();
        Mabl mabl = new Mabl(directory, workingDirectory);
        mabl.setReporter(errorReporter);
        mabl.setVerbose(true);

        // Read JSON
        File executableMMJson =
                Objects.requireNonNull(directory.listFiles((dir, fileName) -> fileName.equals("executableMM.json")))[0];
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode executableMM = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(executableMMJson.getPath()))));

        // Setup values from JSON
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("fmus")), new TypeReference<>() {
                });
        simulationConfiguration.connections =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("connections")), new TypeReference<>() {
                });

        Map<String, Object> parameters =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("multiModel").get("parameters")), new TypeReference<>() {
                });
        Double relTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceRelativeTolerance")),
                new TypeReference<>() {
                });
        Double absTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceAbsoluteTolerance")),
                new TypeReference<>() {
                });
        Integer convergenceAttempts =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("convergenceAttempts")),
                        new TypeReference<>() {
                        });
        Double startTime =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("startTime")), new TypeReference<>() {
                });
        Double endTime = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("endTime")), new TypeReference<>() {
        });
        Double stepSize =
                jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("executionParameters").get("stepSize")), new TypeReference<>() {
                });

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableMM.get("masterModel").textValue().getBytes()));

        // Setup scenarioConfiguration
        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, errorReporter);
        ScenarioConfiguration scenarioConfiguration =
                new ScenarioConfiguration(simulationEnvironment, masterModel, parameters, relTol, absTol, convergenceAttempts, startTime, endTime,
                        stepSize, Pair.of(Framework.FMI2, simulationConfiguration), false);

        // ACT
        // This calls TemplateGeneratorFromScenario.generateTemplate which is the method to test
        mabl.generateSpec(scenarioConfiguration);

        mabl.expand();
        mabl.typeCheck();
        mabl.verify(Framework.FMI2);

        // ASSERT
        if (errorReporter.getErrorCount() > 0) {
            errorReporter.printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        if (errorReporter.getWarningCount() > 0) {
            errorReporter.printWarnings(new PrintWriter(System.out, true));
        }
        PrettyPrinter.print(mabl.getMainSimulationUnit());
        mabl.dump(workingDirectory);
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_FILENAME).exists(), "Spec file must exist");
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_RUNTIME_FILENAME).exists(), "Spec file must exist");
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());


        csvCompare(new File(directory, "expectedoutputs.csv"), new File(workingDirectory, "outputs.csv"));
    }
}
