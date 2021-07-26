package org.intocps.maestro.webapi.maestro2;

import cli.VerifyTA;
import core.MasterModel;
import core.ModelEncoding;
import core.ScenarioGenerator;
import core.ScenarioLoader;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.webapi.MasterModelMapper;
import org.intocps.maestro.webapi.dto.ExecutableMasterAndMultiModelTDO;
import org.intocps.maestro.webapi.dto.MasterMultiModelDTO;
import org.intocps.maestro.webapi.dto.VerificationDTO;
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier;
import org.intocps.maestro.webapi.util.Files;
import org.intocps.maestro.webapi.util.ZipDirectory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import scala.jdk.javaapi.CollectionConverters;
import synthesizer.ConfParser.ScenarioConfGenerator;
import trace_analyzer.TraceAnalyzer;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

@RestController
@Component
public class Maestro2ScenarioController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE}, produces =
            MediaType.TEXT_PLAIN_VALUE)
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {
        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public MasterMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {
        // MaxPossibleStepSize is related to verification in Uppaal.
        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        return new MasterMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/verifyAlgorithm", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public VerificationDTO verifyAlgorithm(@RequestBody String masterModelAsString) throws Exception {
        // Load the master model, verify the algorithm and return success, detailed message and the resulting uppaal model.
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
        File tempDir = Files.createTempDir();
        File uppaalFile = Path.of(tempDir.getPath(), "uppaal.xml").toFile();
        int resultCode = verifyAlgorithm(masterModel, uppaalFile);
        String uppaalFileAsString = java.nio.file.Files.readString(uppaalFile.toPath());
        return new VerificationDTO(resultCode == 0, verificationCodeToStringMessage(resultCode), uppaalFileAsString);
    }

    @RequestMapping(value = "/visualizeTrace", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void visualizeTrace(@RequestBody String masterModelAsString, HttpServletResponse response) throws Exception {
        if (!VerifyTA.checkEnvironment()) {
            throw new Exception("Verification environment is not setup correctly");
        }
        File tempDir = Files.createTempDir();
        try {
            MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
            File uppaalFile = Path.of(tempDir.getPath(), "uppaal.xml").toFile();
            File traceFile = Path.of(tempDir.getPath(), "trace.log").toFile();
            try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
                fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
            } catch (Exception e) {
                throw new Exception("Unable to write encoded master model to file: " + e);
            }
            // This verifies the algorithm and writes to the trace file.
            int resultCode = VerifyTA.saveTraceToFile(uppaalFile, traceFile);

            // If verification result code is 1 violations of the scenarios' contract were found and a trace has been written to the trace file
            // from which a visualization can be made.
            if (resultCode == 1) {
                Path videoTraceFolder = java.nio.file.Files.createDirectories(Path.of(tempDir.getPath(), "video_trace"));
                ModelEncoding modelEncoding = new ModelEncoding(masterModel);
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile))) {
                    TraceAnalyzer.AnalyseScenario(masterModel.name(), CollectionConverters.asScala(bufferedReader.lines().iterator()), modelEncoding,
                            videoTraceFolder.toString());

                } catch (Exception e) {
                    throw new Exception("Unable to generate trace visualization: " + e);
                }

                // Setting headers
                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader("Content-Disposition", "attachment; filename=\"traceVisualization.zip\"");

                // Return visualization in a zip file as it consist of multiple mp4 files.
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                    ZipDirectory.addDir(videoTraceFolder.toFile(), videoTraceFolder.toFile(), zipOutputStream);
                }
            }
            // If the verification code is anything else than 1 it is not possible to visualize the trace and this is an "error" for this endpoint
            // even though the verification might have been successful.
            else {
                throw new Exception(verificationCodeToStringMessage(resultCode));
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel, HttpServletResponse response) throws Exception {

        // We only verify the algorithm if the verification flag is set.
        if (executableModel.getMultiModel().scenarioVerifier.verification) {
            File tempDir = Files.createTempDir();
            MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes()));
            int resultCode = verifyAlgorithm(masterModel, Path.of(tempDir.getPath(), "uppaal.xml").toFile());
            FileUtils.deleteDirectory(tempDir);

            if (resultCode != 0) {
                throw new Exception(verificationCodeToStringMessage(resultCode));
            }
        }

        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = executableModel.getMultiModel().getFmus();
        simulationConfiguration.connections = executableModel.getMultiModel().getConnections();

        if (simulationConfiguration.fmus == null) {
            throw new IllegalArgumentException("Missing FMUs in multi model");
        }

        File zipDir = Files.createTempDir();
        try {
            ErrorReporter reporter = new ErrorReporter();
            Maestro2Broker broker = new Maestro2Broker(zipDir, reporter);

            broker.buildAndRunExecutableModel(executableModel, new File(zipDir, "outputs.csv"));

            if (reporter.getErrorCount() > 0) {
                throw new Exception("Error(s) occurred during MaBL specification generation: " + reporter);
            }

            File warningsLog = Path.of(zipDir.getPath(), "specGenWarnings.log").toFile();

            if (reporter.getWarningCount() > 0) {
                reporter.printWarnings(new PrintWriter(warningsLog));
            }

            // Setting headers
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Content-Disposition", "attachment; filename=\"results.zip\"");

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                ZipDirectory.addDir(zipDir, zipDir, zipOutputStream);
            }

        } finally {
            FileUtils.deleteDirectory(zipDir);
        }
    }

    private String verificationCodeToStringMessage(int verificationCode) throws Exception {
        if (verificationCode == 2) {
            return "Unable to verify algorithm - there is probably a syntax error.";
        } else if (verificationCode == 1) {
            return "Violations of the scenarios' contract were found - traces can be generated.";
        } else if (verificationCode == 0) {
            return "Algorithm successfully verified - no traces to visualize.";
        }

        throw new Exception("Unknown algorithm verification error code encountered: " + verificationCode);
    }

    private int verifyAlgorithm(MasterModel masterModel, File uppaalFile) throws Exception {
        if (VerifyTA.checkEnvironment()) {
            try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
                fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
            } catch (Exception e) {
                throw new Exception("Unable to write encoded master model to file: " + e);
            }
            return VerifyTA.verify(uppaalFile);
        }
        throw new Exception("Verification environment is not setup correctly");
    }
}
