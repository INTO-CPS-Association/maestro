package org.intocps.maestro.webapi.maestro2;
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2;
import org.intocps.verification.scenarioverifier.core.masterModel.*;
import org.intocps.verification.scenarioverifier.core.ModelEncoding;
import org.intocps.verification.scenarioverifier.core.ScenarioGenerator;
import org.intocps.verification.scenarioverifier.api.TraceResult;
import org.intocps.verification.scenarioverifier.api.VerificationAPI;
import org.intocps.verification.scenarioverifier.cli.VerifyTA;
import org.intocps.maestro.core.dto.ExtendedMultiModel;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.plugin.MasterModelMapper;
import org.intocps.maestro.webapi.dto.ExecutableMasterAndMultiModelTDO;
import org.intocps.maestro.webapi.maestro2.dto.SigverSimulateRequestBody;
import org.intocps.maestro.webapi.maestro2.dto.VerificationResult;
import org.intocps.maestro.webapi.util.ZipDirectory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@RestController
@Component
public class Maestro2ScenarioController {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleJsonParseException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return new ResponseEntity(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {
        MasterModel masterModel = MasterModelMapper.Companion.scenarioToFMI2MasterModel(scenario);
        return masterModel.toConf(0);
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String generateAlgorithmFromMultiModel(@RequestBody ExtendedMultiModel multiModel) {
        // MaxPossibleStepSize is related to verification in Uppaal.
        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        return masterModel.toConf(0);
    }

    @RequestMapping(value = "/verifyAlgorithm", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public VerificationResult verifyAlgorithm(@RequestBody String masterModelAsString) throws IOException {
        // Check that UPPAAL is available
        try {
            VerifyTA.isInstalled();
        } catch (Exception e) {
            return new VerificationResult(false, "", "UPPAAL v.4.1 is not in PATH - please install it and try again!");
        }

        // Load the master model, verify the algorithm and return success and any error message.
        MasterModelFMI2 masterModel = (MasterModelFMI2)ScenarioLoaderFMI2.load(new ByteArrayInputStream(masterModelAsString.getBytes()));

        ModelEncoding encoding = new ModelEncoding(masterModel);
        String encodedModel = ScenarioGenerator.generateUppaalEncoding(encoding);
        File tempFile = Files.createTempFile(null, ".xml").toFile();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write(encodedModel);
        }
        int verificationCode = VerifyTA.verify(tempFile, false);
        tempFile.delete();
        switch (verificationCode) {
            case 0:
                return new VerificationResult(true, encodedModel, "");
            case 1:
                return new VerificationResult(false, encodedModel, "Model is not valid. Generate the trace and use it to correct the algorithm.");
            case 2:
                return new VerificationResult(false, encodedModel,
                        "The verification in Uppaal failed most likely due to a syntax error in the " + "UPPAAL model.");
            default:
                return new VerificationResult(false, encodedModel, "");
        }
    }

    @RequestMapping(value = "/visualizeTrace", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE, produces = "video/mp4")
    public FileSystemResource visualizeTrace(@RequestBody String masterModelAsString) throws Exception {
        MasterModel masterModel = (MasterModelFMI2)ScenarioLoaderFMI2.load(new ByteArrayInputStream(masterModelAsString.getBytes()));
        TraceResult traceResult = VerificationAPI.generateTraceVideo(masterModel);

        if (!traceResult.isGenerated()) {
            throw new Exception("Unable to generate trace results - the algorithm is probably successfully verified");
        }

        return new FileSystemResource(traceResult.file());
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void executeAlgorithm(@RequestBody ExecutableMasterAndMultiModelTDO executableModel, HttpServletResponse response) throws Exception {

        if (executableModel.getMultiModel().getFmus() == null || executableModel.getMultiModel().getFmus().isEmpty()) {
            throw new IllegalArgumentException("Missing FMUs in multi model");
        }

        MasterModel masterModel;
        if (executableModel.getMasterModel().equals("")) {
            masterModel = MasterModelMapper.Companion.multiModelToMasterModel(executableModel.getMultiModel(), 3);
        } else {
            masterModel = ScenarioLoaderFMI2.load(new ByteArrayInputStream(executableModel.getMasterModel().getBytes()));
        }

        // Only verify the algorithm if the verification flag is set.
        if (executableModel.getMultiModel().sigver.verification) {
            if (!VerificationAPI.verifyAlgorithm(masterModel)) {
                throw new Exception("Algorithm did not verify successfully - unable to execute it");
            }
        }

        File zipDir = Files.createTempDirectory(null).toFile();
        try {
            ErrorReporter reporter = new ErrorReporter();
            Maestro2Broker broker = new Maestro2Broker(zipDir, reporter, () -> false);

            SigverSimulateRequestBody requestBody = new SigverSimulateRequestBody(executableModel.getExecutionParameters().getStartTime(),
                    executableModel.getExecutionParameters().getEndTime(), Map.of(), false, 0d, masterModel.toConf(0));

            broker.buildAndRunMasterModel(null, null, executableModel.getMultiModel(), requestBody, new File(zipDir, "outputs.csv"));

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
            zipDir.delete();
        }
    }
}
