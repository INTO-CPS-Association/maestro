package org.intocps.maestro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.MasterModel;
import core.ScenarioLoader;
import org.apache.commons.io.IOUtils;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.intocps.maestro.FullSpecTest.getWorkingDirectory;

public class TemplateGeneratorFromScenarioTest {
    /**
     * Data Provider
     *
     * @return
     */
    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "template_generator_from_scenario").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void generateSpec(String name, File directory) throws Exception {

        // ARRANGE
        File workingDirectory = getWorkingDirectory(directory);
        IErrorReporter errorReporter = new ErrorReporter();
        Mabl mabl = new Mabl(directory, workingDirectory);
        mabl.setReporter(errorReporter);
        mabl.setVerbose(true);

        // Read JSON
        File executableMMJson =
                Objects.requireNonNull(directory.listFiles((dir, name1) -> name1.startsWith("executableMM") && name1.endsWith("json")))[0];
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode executableMM = jsonMapper.readTree(new String(Files.readAllBytes(Paths.get(executableMMJson.getPath()))));

        // Setup values from JSON
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("fmus")), new TypeReference<>() {
        });
        simulationConfiguration.connections = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("connections")), new TypeReference<>() {
        });
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableMM.get("masterModel").binaryValue()));
        Map<String, Object> parameters = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("parameters")), new TypeReference<>() {
        });
        Double relTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("convergenceRelativeTolerance")), new TypeReference<>() {
        });
        Double absTol = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("convergenceAbsoluteTolerance")), new TypeReference<>() {
        });
        Integer convergenceAttempts = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("convergenceAttempts")), new TypeReference<>() {
        });
        Double startTime = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("startTime")), new TypeReference<>() {
        });
        Double endTime = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("endTime")), new TypeReference<>() {
        });
        Double stepSize = jsonMapper.readValue(jsonMapper.treeAsTokens(executableMM.get("stepSize")), new TypeReference<>() {
        });

        // Setup scenarioConfiguration
        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, errorReporter);
        ScenarioConfiguration scenarioConfiguration =
                new ScenarioConfiguration(simulationEnvironment, masterModel, parameters, relTol, absTol, convergenceAttempts, startTime, endTime,
                        stepSize);

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

        mabl.dump(workingDirectory);
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_FILENAME).exists(), "Spec file must exist");
        Assertions.assertTrue(new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_RUNTIME_FILENAME).exists(), "Spec file must exist");
        System.out.println(PrettyPrinter.print(mabl.getMainSimulationUnit()));
        new MableInterpreter(
                new DefaultExternalValueFactory(workingDirectory, IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8)))
                .execute(mabl.getMainSimulationUnit());


        compareCSVs(new File(directory, "expectedoutputs.csv"), new File(workingDirectory, "outputs.csv"));
    }

    protected void compareCSVs(File expectedCsvFile, File actualCsvFile) throws IOException {
        final String ROW_SEPARATOR = ",";
        boolean actualOutputsCsvExists = actualCsvFile.exists();
        boolean expectedOutputsCsvExists = expectedCsvFile.exists();

        if (actualOutputsCsvExists && expectedOutputsCsvExists) {

            //Map content of expectedCsvFile to list of data foreach column.
            List<String> csvFileLines = Files.readAllLines(expectedCsvFile.toPath(), StandardCharsets.UTF_8);
            Map<String, List<String>> expectedCsvFileColumnsMap = new HashMap<>();
            Map<Integer, String> columnNameToColumnIndex = new HashMap<>();
            for (int i = 0; i < csvFileLines.size(); i++) {
                int columnIndex = 0;
                for (String columnVal : csvFileLines.get(i).split(ROW_SEPARATOR)) {
                    if (i == 0) {
                        expectedCsvFileColumnsMap.put(columnVal.strip(), new ArrayList<>()); //Strip accidental white spaces
                        columnNameToColumnIndex.put(columnIndex, columnVal.strip());
                    } else {
                        expectedCsvFileColumnsMap.get(columnNameToColumnIndex.get(columnIndex)).add(columnVal.strip());
                    }
                    columnIndex++;
                }
            }

            //Map content of actualCsvFile to list of data foreach column.
            columnNameToColumnIndex = new HashMap<>();
            csvFileLines = Files.readAllLines(actualCsvFile.toPath(), StandardCharsets.UTF_8);
            Map<String, List<String>> actualCsvFileColumnsMap = new HashMap<>();
            for (int i = 0; i < csvFileLines.size(); i++) {
                int columnIndex = 0;
                for (String columnVal : csvFileLines.get(i).split(ROW_SEPARATOR)) {
                    if (i == 0) {
                        actualCsvFileColumnsMap.put(columnVal, new ArrayList<>());
                        columnNameToColumnIndex.put(columnIndex, columnVal);
                    } else {
                        actualCsvFileColumnsMap.get(columnNameToColumnIndex.get(columnIndex)).add(columnVal);
                    }
                    columnIndex++;
                }
            }


            // Validate that columns are equal
            if (actualCsvFileColumnsMap.keySet().size() != expectedCsvFileColumnsMap.keySet().size()) {
                System.out.println("CSV files do not contain the same amount of columns");
            } else if (!actualCsvFileColumnsMap.keySet().containsAll(expectedCsvFileColumnsMap.keySet())) {
                System.out.println("CSV files do not contain the same columns");
            } else {
                // Validate that columns contains the same elements
                for (Map.Entry<String, List<String>> entry : expectedCsvFileColumnsMap.entrySet()) {
                    List<String> expected = entry.getValue();
                    List<String> actual = actualCsvFileColumnsMap.get(entry.getKey());

                    if (expected.size() != actual.size()) {
                        System.out.println("The length of column " + entry.getKey() + " differs between expected and actual");
                    } else {
                        String assertionMsg = "";
                        int mismatchedLines = 0;
                        for (int i = 0; i < expected.size(); i++) {
                            if (!expected.get(i).equals(actual.get(i))) {
                                if (assertionMsg.isEmpty()) {
                                    assertionMsg = "Mismatch between values on row " + (i + 2) + " for column '" + entry.getKey() + "'. Actual: " +
                                            actual.get(i) + " " + "expected: " + expected.get(i) + ". ";
                                }
                                mismatchedLines++;
                            }
                        }

                        if (!assertionMsg.isEmpty()) {
                            assertionMsg += "Additional " + (mismatchedLines - 1) + " rows have mismatched values.";
                            System.out.println(assertionMsg);
                        }
                    }
                }
            }
        } else {

            StringBuilder sb = new StringBuilder();

            sb.append("Cannot compare CSV files.\n");
            if (!actualOutputsCsvExists) {
                sb.append("The actual outputs csv file does not exist.\n");
            }
            if (!expectedOutputsCsvExists) {
                sb.append("The expected outputs csv file does not exist.\n");
            }
            System.out.println(sb);
        }
    }
}
