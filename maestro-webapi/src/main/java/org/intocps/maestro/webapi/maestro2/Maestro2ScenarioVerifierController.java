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
import org.intocps.maestro.webapi.controllers.SessionController;
import org.intocps.maestro.webapi.dto.ExecutableMasterAndMultiModelTDO;
import org.intocps.maestro.webapi.dto.MasterMultiModelDTO;
import org.intocps.maestro.webapi.dto.verificationDTO;
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
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE})
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {
        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public MasterMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {
        // MaxPossibleStepSize is related to verification in Uppaal.
        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        return new MasterMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/verifyAlgorithm", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE})
    public verificationDTO verifyAlgorithm(@RequestBody String masterModelAsString) throws Exception {
        //TODO: should the trace log be returned if present?

        // Load the master model and verify the algorithm
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
        int resultCode = verifyAlgorithm(masterModel, Files.createTempDir());
        return new verificationDTO(resultCode == 0, verificationCodeToStringMessage(resultCode));
    }

    @RequestMapping(value = "/traceVisualization", method = RequestMethod.POST, consumes = {MediaType.TEXT_PLAIN_VALUE})
    public String traceVisualization(@RequestBody String masterModelAsString) throws Exception {
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
            int resultCode =  VerifyTA.saveTraceToFile(uppaalFile, traceFile);

            // If verification result code is 1 one or more counter example has been found and written to the trace file from which a visualization
            // can be made.
            if (resultCode == 1) {
                File outputFolder = Path.of(tempDir.getPath(), "video_trace").toFile();
                ModelEncoding modelEncoding = new ModelEncoding(masterModel);
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile))) {
                    TraceAnalyzer.AnalyseScenario(masterModel.name(), CollectionConverters.asScala(bufferedReader.lines().iterator()), modelEncoding,
                            outputFolder.getPath());

                } catch (Exception e) {
                    throw new Exception("Unable to generate trace visualization: " + e);
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

        //TODO: Return mp4 file.
        return null;
    }


    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel, HttpServletResponse response) throws Exception {

        // We only verify the algorithm if the verification flag is set.
        if (executableModel.getMultiModel().scenarioVerifier.verification) {
            File tempDir = Files.createTempDir();
            MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes()));
            int resultCode = verifyAlgorithm(masterModel, tempDir);
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

            //setting headers
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Content-Disposition", "attachment; filename=\"results.zip\"");
            //
            ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
            ZipDirectory.addDir(zipDir, zipDir, zipOutputStream);
            zipOutputStream.close();
        } finally {
            FileUtils.deleteDirectory(zipDir);
        }
    }

    private String verificationCodeToStringMessage(int verificationCode) throws Exception {
        if (verificationCode == 2) {
            return "Unable to verify algorithm - there is probably a syntax error.";
        } else if (verificationCode == 1) {
            return "One or more counter examples found.";
        } else if (verificationCode == 0) {
            return "Algorithm successfully verified - no traces to visualize.";
        }

        throw new Exception("Unknown algorithm verification error code encountered: " + verificationCode);
    }

    private int verifyAlgorithm(MasterModel masterModel, File dir) throws Exception {
        if (VerifyTA.checkEnvironment()) {
            File uppaalFile = Path.of(dir.getPath(), "uppaal.xml").toFile();

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
