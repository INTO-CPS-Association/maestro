package org.intocps.maestro.webapi.maestro2;

import api.TraceResult;
import api.VerificationAPI;
import core.*;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.plugin.MasterModelMapper;
import org.intocps.maestro.webapi.dto.ExecutableMasterAndMultiModelTDO;
import org.intocps.maestro.webapi.dto.MasterMultiModelDTO;
import org.intocps.maestro.core.dto.ExtendedMultiModel;
import org.intocps.maestro.webapi.dto.VerificationDTO;
import org.intocps.maestro.webapi.util.Files;
import org.intocps.maestro.webapi.util.ZipDirectory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import synthesizer.ConfParser.ScenarioConfGenerator;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

@RestController
@Component
public class Maestro2ScenarioController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {
        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MasterMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody ExtendedMultiModel multiModel) {
        // MaxPossibleStepSize is related to verification in Uppaal.
        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        return new MasterMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/verifyAlgorithm", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public VerificationDTO verifyAlgorithm(@RequestBody String masterModelAsString) {
        // Load the master model, verify the algorithm and return success and any error message.
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
        boolean didVerify = false;
        String errorMessage = "";
        try {
            didVerify = VerificationAPI.verifyAlgorithm(masterModel);
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
        return new VerificationDTO(didVerify, errorMessage);
    }

    @RequestMapping(value = "/visualizeTrace", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE, produces = "video/mp4")
    public FileSystemResource visualizeTrace(@RequestBody String masterModelAsString) throws Exception {
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
        TraceResult traceResult = VerificationAPI.generateTraceFromMasterModel(masterModel);

        if (!traceResult.isGenerated()) {
            throw new Exception("Unable to generate trace results - the algorithm is probably successfully verified");
        }

        return new FileSystemResource(traceResult.file());
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel, HttpServletResponse response) throws Exception {

        // Only verify the algorithm if the verification flag is set.
        if (executableModel.getMultiModel().scenarioVerifier.verification &&
                !verifyAlgorithm(executableModel.getMasterModel()).getVerifiedSuccessfully()) {
            throw new Exception("Algorithm did not verify successfully - unable to execute it");
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

            broker.buildAndRunMasterModel(executableModel.getMultiModel(), executableModel.getMasterModel(), executableModel.getExecutionParameters(),
                    new File(zipDir, "outputs.csv"));

            if (reporter.getErrorCount() > 0) {
                throw new Exception("Error(s) occurred during MaBL specification generation: " + reporter);
            }

            if (reporter.getWarningCount() > 0) {
                PrintWriter printWriter = new PrintWriter(Path.of(zipDir.getPath(), "Specification-generation-warnings.log").toFile());
                reporter.printWarnings(printWriter);
                printWriter.close();
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
}
