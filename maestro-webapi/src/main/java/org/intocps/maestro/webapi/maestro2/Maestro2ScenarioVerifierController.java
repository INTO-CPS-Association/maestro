package org.intocps.maestro.webapi.maestro2;

import cli.VerifyTA;
import core.MasterModel;
import core.ModelEncoding;
import core.ScenarioGenerator;
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
import org.intocps.maestro.webapi.dto.MasterMultiModelDTO;
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import scala.jdk.javaapi.CollectionConverters;
import synthesizer.ConfParser.ScenarioConfGenerator;
import trace_analyzer.TraceAnalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {"text/plain"})
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {
        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {"application/json"})
    public MasterMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {
        // MaxPossibleStepSize is related to verification in Uppaal.
        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        return new MasterMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/verifyAlgorithm", method = RequestMethod.POST, consumes = {"text/plain"})
    public int verifyAlgorithm(@RequestBody String masterModel) throws Exception {
        //TODO: return possible error code or a text format instead?
        return verifyAlgorithmFromMasterModel(ScenarioLoader.load(new ByteArrayInputStream(masterModel.getBytes())), createTempDir());
    }

    @RequestMapping(value = "/traceVisualization", method = RequestMethod.POST, consumes = {"text/plain"})
    public String traceVisualization(@RequestBody String masterModelFromString) throws Exception {
        File tempDir = createTempDir();

        if (VerifyTA.checkEnvironment()) {
            File file = Path.of(tempDir.getPath(), "uppaal.xml").toFile();
            MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelFromString.getBytes()));
            ModelEncoding queryModel = new ModelEncoding(masterModel);
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(ScenarioGenerator.generate(queryModel));
            } catch (Exception e) {
                throw new Exception("Unable to write query model to file: " + e);
            }
            File traceFile = Path.of(tempDir.getPath(), "trace.log").toFile();
            int result = VerifyTA.saveTraceToFile(file, traceFile);
            if (result == 2) {
                //TODO: Log?
            } else if (result == 1) {
                File outputFolder = Path.of(tempDir.getPath(), "video_trace").toFile();
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile))) {

                    //TODO: This should probably just return whatever is needed for a UppaalTrace to enable visualization in the client.
                    TraceAnalyzer.AnalyseScenario(masterModel.name(), CollectionConverters.asScala(bufferedReader.lines().iterator()), queryModel,
                            outputFolder.getPath());
                } catch (Exception e) {
                    throw new Exception("Unable to analyse scenario: " + e);
                }
            } else if (result != 0) {
                throw new Exception("Unknown error code encountered when verifying algorithm: " + result);
            }
        } else {
            throw new Exception("Verification environment is not setup correctly");
        }

        return "";
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel) throws Exception {
        File tempDir = createTempDir();

        if (executableModel.getMultiModel().scenarioVerifier.verification) {
            int result = verifyAlgorithmFromMasterModel(ScenarioLoader.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes())),
                    tempDir);
            if (result == 2) {
                //TODO: Notify and log or stop?
            } else if (result == 1) {
                //TODO: Notify and log or stop?
            } else if (result != 0) {
                throw new Exception("Unknown error code encountered when verifying algorithm: " + result);
            }
        }

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes()));
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = executableModel.getMultiModel().getFmus();
        simulationConfiguration.connections = executableModel.getMultiModel().getConnections();

        if (simulationConfiguration.fmus == null) {
            throw new Exception("Missing FMUs from multi-model");
        }

        ErrorReporter reporter = new ErrorReporter();
        Maestro2Broker broker = new Maestro2Broker(tempDir, reporter);
        Fmi2SimulationEnvironment environment = Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);
        broker.generateSpecification(new ScenarioConfiguration(environment, masterModel, executableModel.getMultiModel().getParameters(),
                executableModel.getExecutionParameters().getConvergenceRelativeTolerance(),
                executableModel.getExecutionParameters().getConvergenceAbsoluteTolerance(),
                executableModel.getExecutionParameters().getConvergenceAttempts(), executableModel.getExecutionParameters().getStartTime(),
                executableModel.getExecutionParameters().getEndTime(), executableModel.getExecutionParameters().getStepSize()));

        broker.mabl.typeCheck();
        broker.mabl.verify(Framework.FMI2);

        //TODO: How should errors and warnings be handled?
        if (reporter.getErrorCount() > 0) {
            throw new RuntimeException("Error occurred during spec generation: " + reporter);
        }

        if (reporter.getWarningCount() > 0) {
            reporter.printWarnings(new PrintWriter(System.out, true));
        }

        new MableInterpreter(new DefaultExternalValueFactory(broker.workingDirectory,
                IOUtils.toInputStream(broker.mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8)))
                .execute(broker.mabl.getMainSimulationUnit());

        //TODO: What should be done after execution?
        return null;
    }

    private int verifyAlgorithmFromMasterModel(MasterModel masterModel, File tempDir) throws Exception {
        if (VerifyTA.checkEnvironment()) {
            File file = Path.of(tempDir.getPath(), "uppaal.xml").toFile();

            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
            } catch (Exception e) {
                throw new Exception("Unable to write query model to file: " + e);
            }
            File traceFile = Path.of(tempDir.getPath(), "trace.log").toFile();
            return VerifyTA.saveTraceToFile(file, traceFile);
        }
        throw new Exception("Verification environment is not setup correctly");
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
}
