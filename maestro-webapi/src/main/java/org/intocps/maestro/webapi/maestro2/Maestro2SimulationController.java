package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.intocps.maestro.cli.MablCmdVersionProvider;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.MableError;
import org.intocps.maestro.core.messages.MableWarning;
import org.intocps.maestro.core.dto.FixedStepAlgorithmConfig;
import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
import org.intocps.maestro.core.dto.VariableStepAlgorithmConfig;
import org.intocps.maestro.webapi.Application;
import org.intocps.maestro.webapi.controllers.JavaProcess;
import org.intocps.maestro.webapi.controllers.ProdSessionLogicFactory;
import org.intocps.maestro.webapi.controllers.SessionController;
import org.intocps.maestro.webapi.controllers.SessionLogic;
import org.intocps.maestro.webapi.maestro2.dto.*;
import org.intocps.maestro.webapi.util.ZipDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;


@RestController
@Component
public class Maestro2SimulationController {

    public static final SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(Maestro2SimulationController.class);

    public void overrideRootLoggerLogLevel(Level level) {
        if (level == null) {
            return;
        }
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    @RequestMapping(value = "/upload/{sessionId}", method = RequestMethod.POST)
    public void uploadFile(@PathVariable String sessionId,
            @ApiParam(value = "File", required = true) @RequestParam("fieldFile") MultipartFile file) throws IOException {
        throw new NotImplementedException("/upload/{sessionId} has not been implemented.");
        //        try (InputStream is = file.getInputStream()) {
        //            logger.debug("Uploaded file: {}", file.getOriginalFilename());
        //            File targetFile = new File(sessions.get(sessionId).getResultRoot(), file.getOriginalFilename());
        //            IOUtils.copy(is, new FileOutputStream(targetFile));
        //        }

    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String ping() {
        return "OK";
    }

    @RequestMapping(value = "/status/{sessionId}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getStatuses(@PathVariable String sessionId) throws Exception {
        return new ObjectMapper().writeValueAsString(sessionController.getStatus(sessionId));
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getStatuses() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(sessionController.getStatus());
    }

    private StatusModel getStatus(String sessionId) {
        if (sessionController.containsSession(sessionId)) {
            return new StatusModel("Session exists", sessionId, 0);
        } else {
            return new StatusModel("Session does not exist", sessionId, 0);
        }
    }

    @RequestMapping(value = "/createSession", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusModel createSession() {
        String session = sessionController.createNewSession();
        return getStatus(session);
    }

    @RequestMapping(value = "/initialize/{sessionId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public InitializeStatusModel initializeSession(@PathVariable String sessionId, @RequestBody InitializationData body) throws Exception {
        // Store this data to be used for the interpretor later on.
        // It is not possible to create the spec at this point in time as data for setup experiment is missing (i.e. endtime)
        //        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body1));
        logger.debug("Got initial data");
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.writeValue(new File(logic.rootDirectory, "initialize.json"), body);

        if (body == null) {
            throw new Exception("Could not parse configuration: ");
        }

        if (body.getOverrideLogLevel() != null) {
            overrideRootLoggerLogLevel(convertLogLevel(body.getOverrideLogLevel()));
        }

        if (body.getFmus() == null) {
            throw new Exception("FMUs must not be null");
        }

        if (body.getConnections() == null) {
            throw new Exception("Connections must not be null");
        }

        if (body.getAlgorithm() instanceof FixedStepAlgorithmConfig) {
            FixedStepAlgorithmConfig algorithm = (FixedStepAlgorithmConfig) body.getAlgorithm();

            if (algorithm.size == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixed-step size must be an integer or double");
            }

            logger.info("Using Fixed-step size calculator with size = {}", algorithm.size);

        } else if (body.getAlgorithm() instanceof VariableStepAlgorithmConfig) {
            VariableStepAlgorithmConfig algorithm = (VariableStepAlgorithmConfig) body.getAlgorithm();

            if (algorithm.getSize() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "min and max variable-step size must be integers or doubles");
            }

            if (algorithm.getSize()[0] <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "the minimum variable-step size does not conform to the minimum step-size" + " of FMI2");
            }

            if (algorithm.getSize()[1] <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "the maximum variable-step size does not conform to the minimum step-size" + " of FMI2");
            }

            logger.info("Using variable-step size calculator with minimum step-size: {}, maximum step-size: {} and initial step-size: {}",
                    algorithm.getSize()[0], algorithm.getSize()[1], algorithm.getInitsize());
        }

        if (body.isParallelSimulation()) {
            throw new NotImplementedException("ParallelSimulation is not implemented");
        }

        if (body.isHasExternalSignals()) {
            throw new NotImplementedException("HasExternalSignals is not implemented");
        }


        logger.trace("Initialization completed");
        logic.setInitializationData(body);
        logic.setStatus(SessionLogic.SessionStatus.Initialized);

        return new InitializeStatusModel("initialized", sessionId, null, 0);
    }

    private Level convertLogLevel(InitializationData.InitializeLogLevel overrideLogLevel) {
        switch (overrideLogLevel) {

            case OFF:
                return Level.OFF;
            case FATAL:
                return Level.FATAL;
            case ERROR:
                return Level.ERROR;
            case WARN:
                return Level.WARN;
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.DEBUG;
            case TRACE:
                return Level.TRACE;
            case ALL:
                return Level.ALL;
        }
        return null;
    }


    @ApiOperation(value = "This request begins the co-simulation")
    @RequestMapping(value = "/simulate/{sessionId}", method = RequestMethod.POST, consumes = {"text/plain", "application/json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusModel simulate(@PathVariable String sessionId, @RequestBody SimulateRequestBody body) throws Exception {
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        mapper.writeValue(new File(logic.rootDirectory, "simulate.json"), body);

        ErrorReporter reporter = new ErrorReporter();
        long preSimTime;
        long postSimTime;
        logic.setStatus(SessionLogic.SessionStatus.Simulating);

        if (!logic.getCliExecution()) {
            preSimTime = System.currentTimeMillis();
            Maestro2Broker mc = new Maestro2Broker(logic.rootDirectory, reporter);
            mc.buildAndRun(logic.getInitializationData(), body, logic.getSocket(), new File(logic.rootDirectory, "outputs.csv"));
            postSimTime = System.currentTimeMillis();
            logic.setExecTime(postSimTime - preSimTime);

            if (reporter.getErrorCount() > 0) {
                logic.setStatus(SessionLogic.SessionStatus.Error);
                reporter.getErrors().forEach(x -> logger.error(x.toString()));
                StringWriter out = new StringWriter();
                PrintWriter writer = new PrintWriter(out);
                reporter.printWarnings(writer);
                reporter.printErrors(writer);
                throw new Exception(out.toString());
            }

            reporter.getWarnings().forEach(x -> logger.warn(x.toString()));
            logic.setStatus(SessionLogic.SessionStatus.Finished);
            return new StatusModel("Simulation completed", sessionId, 0,
                    reporter.getErrors().stream().map(MableError::toString).collect(Collectors.toList()),
                    reporter.getWarnings().stream().map(MableWarning::toString).collect(Collectors.toList()));
        } else {
            String simulateJsonPath = new File(logic.rootDirectory, "simulate.json").getAbsolutePath();
            String initializeJsonPath = new File(logic.rootDirectory, "initialize.json").getAbsolutePath();
            String dumpDirectory = logic.rootDirectory.getAbsolutePath();
            List<String> arguments = new ArrayList<>(
                    List.of("cliMain", "--dump-simple", dumpDirectory, "--dump-intermediate", dumpDirectory, "-sg1", simulateJsonPath,
                            initializeJsonPath, "-i", "-v", "FMI2"));

            List<String> error = new ArrayList<>();
            List<String> out = new ArrayList<>();
            List<String> command = JavaProcess.calculateCommand(Application.class, Arrays.asList(), arguments);
            logger.info("Executing command: " + String.join(" ", command));
            preSimTime = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec(command.toArray(String[]::new));
            Scanner outputStreamSc = new Scanner(p.getInputStream());
            Scanner errorStream = new Scanner(p.getErrorStream());

            while (outputStreamSc.hasNextLine() || errorStream.hasNext()) {
                if (outputStreamSc.hasNextLine()) {
                    String outLine = outputStreamSc.nextLine();
                    out.add(outLine);
                    System.out.println(outLine);
                }
                if (errorStream.hasNext()) {
                    var line = errorStream.nextLine();
                    error.add(line);
                    System.out.println(line);
                }
            }
            int result = p.waitFor();

            if (error.size() > 0) {
                logic.setStatus(SessionLogic.SessionStatus.Error);
            } else {
                logic.setStatus(SessionLogic.SessionStatus.Finished);
            }
            postSimTime = System.currentTimeMillis();
            logic.setExecTime(postSimTime - preSimTime);

            return new StatusModel(result + " - Simulation completed", sessionId, 0, error, out);
        }

    }

    @RequestMapping(value = "/stopsimulation/{sessionId}", method = RequestMethod.POST)
    public void stop(@PathVariable String sessionId) {
        throw new NotImplementedException("/stopsimulation/{sessionId} has not been implemented.");
        //sessionController.getSessionLogic(sessionId).setStatus(SessionLogic.SessionStatus.Stopping);
        //        if (sessions.containsKey(sessionId)) {
        //            sessions.get(sessionId).stopSimulation();
        //        }
    }

    @RequestMapping(value = "/result/{sessionId}/plain", method = RequestMethod.GET, produces = "text/csv")
    public ResponseEntity<Resource> getResultPlain(@PathVariable String sessionId) throws Exception {
        SessionLogic sessionLogic = this.sessionController.getSessionLogic(sessionId);

        if (sessionLogic == null) {
            throw new IllegalArgumentException("The session with id: " + sessionId + " does not exist.");
        }

        ByteArrayResource resource = new ByteArrayResource(FileUtils.readFileToByteArray(new File(sessionLogic.rootDirectory, "outputs.csv")));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "outputs.csv" + "\"").body(resource);
    }

    @RequestMapping(value = "/result/{sessionId}/zip", method = RequestMethod.GET, produces = "application/zip")
    public void getResultZip(@PathVariable String sessionId, HttpServletResponse response) throws Exception {
        SessionLogic sessionLogic = this.sessionController.getSessionLogic(sessionId);

        if (sessionLogic == null) {
            throw new IllegalArgumentException("The session with id: " + sessionId + " does not exist.");
        }

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"results.zip\"");
        //
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
        ZipDirectory.addDir(sessionLogic.rootDirectory, sessionLogic.rootDirectory, zipOutputStream);
        zipOutputStream.close();
    }

    @RequestMapping(value = "/destroy/{sessionId}", method = RequestMethod.GET)
    public void destroy(@PathVariable String sessionId) throws Exception {
        this.sessionController.deleteSession(sessionId);
    }

    @RequestMapping(value = "/version", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String version() {
        final String message;
        try {
            message = "{\"version\":\"" + new MablCmdVersionProvider().getVersion()[0] + "\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
        return message;
    }

    @RequestMapping(value = "/executeViaCLI/{sessionId}", method = RequestMethod.POST)
    public void executeViaCLI(@PathVariable String sessionId, @RequestBody CliExecutionRequestBody cliExecutionRequestBody) {
        sessionController.getSessionLogic(sessionId).setCliExecution(cliExecutionRequestBody.executeViaCLI);
    }


    //    @RequestMapping(value = "/reset/{sessionId}", method = RequestMethod.GET)
    //    public void reset(@PathVariable String sessionId) {
    //
    //    }


}