package org.intocps.maestro.webapi.maestro2;

import core.MasterModel;
import core.ScenarioLoader;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.intocps.maestro.webapi.MasterModelMapper;
import org.intocps.maestro.webapi.dto.ExecutableMasterAndMultiModelTDO;
import org.intocps.maestro.webapi.dto.MasterAndMultiModelDTO;
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import synthesizer.ConfParser.ScenarioConfGenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {"text/plain"})
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {

        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);

        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {"application/json"})
    public MasterAndMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {

        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);

        return new MasterAndMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel) throws Exception {

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes()));
        ErrorReporter reporter = new ErrorReporter();
        Maestro2Broker broker = new Maestro2Broker(createTempDir(), reporter);
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = executableModel.getMultiModel().getFmus();
        simulationConfiguration.connections = executableModel.getMultiModel().getConnections();

        if (simulationConfiguration.fmus == null) {
            throw new Exception("Missing FMUs from multi-model");
        }

        Fmi2SimulationEnvironment environment = Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);

        broker.generateSpecification(new ScenarioConfiguration(environment, masterModel, executableModel.getMultiModel().getParameters(),
                executableModel.getExecutionParameters().getConvergenceRelativeTolerance(), executableModel.getExecutionParameters().getConvergenceAbsoluteTolerance(),
                executableModel.getExecutionParameters().getConvergenceAttempts(), executableModel.getExecutionParameters().getStartTime(),
                executableModel.getExecutionParameters().getEndTime(), executableModel.getExecutionParameters().getStepSize()));

        broker.mabl.typeCheck();
        broker.mabl.verify(Framework.FMI2);

        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
        }

        if (reporter.getWarningCount() > 0) {
            reporter.printWarnings(new PrintWriter(System.out, true));
        }
        //org.intocps.maestro.ast.display.PrettyPrinter.print(broker.mabl.getMainSimulationUnit();

        new MableInterpreter(
                new DefaultExternalValueFactory(broker.workingDirectory, IOUtils.toInputStream(broker.mabl.getRuntimeDataAsJsonString(),
                        StandardCharsets.UTF_8)))
                .execute(broker.mabl.getMainSimulationUnit());

        //TODO: REMOVE. FOR TESTING ONLY..
//        compareCSVs(new File("C:\\Users\\frdrk\\Desktop" +
//                "\\expectedoutputs_legacy_discard.csv"), new File(broker.workingDirectory.getAbsolutePath()+"\\outputs.csv"));
        compareCSVs(new File("C:\\Users\\frdrk\\Desktop" +
                "\\expectedoutputs_legacy_fixed_step.csv"), new File(broker.workingDirectory.getAbsolutePath()+"\\outputs.csv"));

        return null;
    }

    private File createTempDir() {
        int TEMP_DIR_ATTEMPTS = 10000;
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
                "Failed to create directory within " + TEMP_DIR_ATTEMPTS + " attempts (tried " + baseName + "0 to " + baseName +
                        (TEMP_DIR_ATTEMPTS - 1) + ')');
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
            }
            if (!actualCsvFileColumnsMap.keySet().containsAll(expectedCsvFileColumnsMap.keySet())) {
                System.out.println("CSV files do not contain the same columns");
            }

            // Validate that columns contains the same elements
            for (Map.Entry<String, List<String>> entry : expectedCsvFileColumnsMap.entrySet()) {
                List<String> expected = entry.getValue();
                List<String> actual = actualCsvFileColumnsMap.get(entry.getKey());


                if (expected.size() != actual.size()) {
                    System.out.println("The length of column " + entry.getKey() + " differs between expected and actual");
                }

                String assertionMsg = "";
                int mismatchedLines = 0;
                for (int i = 0; i < expected.size(); i++) {
                    if (!expected.get(i).equals(actual.get(i))) {
                        if (assertionMsg.isEmpty()) {
                            assertionMsg =
                                    "Mismatch between values on row " + (i+2) + " for column '" + entry.getKey() + "'. Actual: " + actual.get(i) +
                                            " " +
                                            "expected: " + expected.get(i) + ". ";
                        }
                        mismatchedLines++;
                    }
                }

                if (!assertionMsg.isEmpty()) {
                    assertionMsg += "Additional " + (mismatchedLines - 1) + " rows have mismatched values.";
                    System.out.println(assertionMsg);
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
