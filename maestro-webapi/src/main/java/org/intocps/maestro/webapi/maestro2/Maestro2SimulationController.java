package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.Main;
import org.intocps.maestro.core.dto.FixedStepAlgorithmConfig;
import org.intocps.maestro.core.dto.VariableStepAlgorithmConfig;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.MableError;
import org.intocps.maestro.core.messages.MableWarning;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
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

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;


@RestController
@Component
public class Maestro2SimulationController {

    public static final SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(Maestro2SimulationController.class);
    static String version;

    static {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("maestro.properties");
            prop.load(coeProp);
            version = prop.getProperty("version");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

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
                           @ApiParam(value = "File", required = true) @RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            logger.debug("Uploaded file: {}", file.getOriginalFilename());
            File targetFile = new File(sessionController.getSessionLogic(sessionId).getRootDirectory(), file.getOriginalFilename());
            IOUtils.copy(is, new FileOutputStream(targetFile));
        }

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

    @RequestMapping(value = "/initialize/{sessionId}", method = RequestMethod.POST, consumes = {"text/plain", "application/json"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InitializeStatusModel initializeSession(@PathVariable String sessionId, @RequestBody InitializationData body) throws Exception {
        // Store this data to be used for the interpretor later on.
        // It is not possible to create the spec at this point in time as data for setup experiment is missing (i.e. endtime)
        //        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body1));
        logger.debug("Got initial data");
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        if (body.getAlgorithm() instanceof FixedStepAlgorithmConfig algorithm) {

            if (algorithm.size == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fixed-step size must be an integer or double");
            }

            logger.info("Using Fixed-step size calculator with size = {}", algorithm.size);

        } else if (body.getAlgorithm() instanceof VariableStepAlgorithmConfig algorithm) {

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

        // This functionality only works for the FMUs that the org.intocps.maestro.framework.fmi2.LocalFactory can instantiate.
        HashMap<String, List<InitializeStatusModel.LogLevelModel>> logcategoryKeyToLogCategories = new HashMap<>();
        try {
            // Load the FMUs
            for (Map.Entry<String, String> fmuKeyToFmuURI : body.getFmus().entrySet()) {
                IFmu iFmu = FmuFactory.create(logic.rootDirectory, URI.create(fmuKeyToFmuURI.getValue()));
                Fmi2ModelDescription fmi2ModelDescription = new Fmi2ModelDescription(iFmu.getModelDescription());

                try {
                    List<ModelDescription.LogCategory> logCategories = fmi2ModelDescription.getLogCategories();
                    logcategoryKeyToLogCategories.put(fmuKeyToFmuURI.getKey(),
                            logCategories.stream().map(x -> new InitializeStatusModel.LogLevelModel(x.getName(), x.getDescription()))
                                    .collect(Collectors.toList()));
                } catch (NullPointerException e) {
                    logger.trace("No log categories found for FMU: " + fmuKeyToFmuURI.getKey());
                }

            }
        } catch (Exception e) {
            logger.info("Could not retrieve logging levels for one or more FMUs.");
        }

        return new InitializeStatusModel("initialized", sessionId, logcategoryKeyToLogCategories, 0);
    }

    @ApiOperation(value = "This request executes the algorithm provided in the master model")
    @RequestMapping(value = "/sigverSimulate/{sessionId}", method = RequestMethod.POST, consumes = {"application/json"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusModel sigverSimulate(@PathVariable String sessionId, @RequestBody SigverSimulateRequestBody body) throws Exception {
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        return runSimulation(
                (Maestro2Broker broker) -> broker.buildAndRunMasterModel(logic.getInitializationData().getLivestream(), logic.getSocket(),
                        logic.getInitializationData(), body, new File(logic.rootDirectory, "outputs.csv")), logic, body, sessionId);
    }

    @ApiOperation(value = "This request begins the co-simulation")
    @RequestMapping(value = "/simulate/{sessionId}", method = RequestMethod.POST, consumes = {"text/plain", "application/json"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusModel simulate(@PathVariable String sessionId, @RequestBody SimulateRequestBody body) throws Exception {
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        if (!logic.getCliExecution()) {
            return runSimulation((Maestro2Broker broker) -> broker.buildAndRun(logic.getInitializationData(), body, logic.getSocket(),
                    new File(logic.rootDirectory, "outputs.csv")), logic, body, sessionId);
        } else {
            mapper.writeValue(new File(logic.rootDirectory, "simulate.json"), body);

            long preSimTime;
            long postSimTime;
            logic.setStatus(SessionLogic.SessionStatus.Simulating);
            String simulateJsonPath = new File(logic.rootDirectory, "simulate.json").getAbsolutePath();
            String initializeJsonPath = new File(logic.rootDirectory, "initialize.json").getAbsolutePath();
            String dumpDirectory = logic.rootDirectory.getAbsolutePath();
            List<String> arguments = new ArrayList<>(
                    List.of("cliMain", "import", "-output", dumpDirectory, "--dump-intermediate", "sg1", initializeJsonPath, simulateJsonPath, "-i",
                            "-v", "FMI2", "--fmu-search-path", sessionController.getSessionLogic(sessionId).getRootDirectory().getAbsolutePath()));

            List<String> error = new ArrayList<>();
            List<String> out = new ArrayList<>();
            List<String> command = JavaProcess.calculateCommand(Application.class, List.of(), arguments);
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

            if (!error.isEmpty()) {
                logic.setStatus(SessionLogic.SessionStatus.Error);
            } else {
                logic.setStatus(SessionLogic.SessionStatus.Finished);
            }
            postSimTime = System.currentTimeMillis();
            logic.setExecTime(postSimTime - preSimTime);

            return new StatusModel(result + " - Simulation completed", sessionId, 0, error, out);
        }

    }

    @RequestMapping(value = "/stopsimulation/{sessionId}", method = {RequestMethod.POST, RequestMethod.GET})
    public void stop(@PathVariable String sessionId) {
        SessionLogic sessionLogic = sessionController.getSessionLogic(sessionId);
        if (sessionLogic != null) {
            sessionLogic.setStopRequested(true);
        }
    }

    @RequestMapping(value = "/result/{sessionId}/plain", method = RequestMethod.GET, produces = "text/csv")
    public ResponseEntity<Resource> getResultPlain(@PathVariable String sessionId) throws Exception {
        SessionLogic sessionLogic = sessionController.getSessionLogic(sessionId);

        if (sessionLogic == null) {
            throw new IllegalArgumentException("The session with id: " + sessionId + " does not exist.");
        }

        ByteArrayResource resource = new ByteArrayResource(FileUtils.readFileToByteArray(new File(sessionLogic.rootDirectory, "outputs.csv")));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "outputs.csv" + "\"").body(resource);
    }

    @RequestMapping(value = "/result/{sessionId}/zip", method = RequestMethod.GET, produces = "application/zip")
    public void getResultZip(@PathVariable String sessionId, HttpServletResponse response) throws Exception {
        SessionLogic sessionLogic = sessionController.getSessionLogic(sessionId);

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
            message = "{\"version\":\"" + version + "\"}";
        } catch (Exception e) {
           logger.error("Could not get maestro version.", e);
            return "unknown";
        }
        return message;
    }

    @RequestMapping(value = "/executeViaCLI/{sessionId}", method = RequestMethod.POST)
    public void executeViaCLI(@PathVariable String sessionId, @RequestBody CliExecutionRequestBody cliExecutionRequestBody) {
        sessionController.getSessionLogic(sessionId).setCliExecution(cliExecutionRequestBody.executeViaCLI);
    }

    private Level convertLogLevel(InitializationData.InitializeLogLevel overrideLogLevel) {
        return switch (overrideLogLevel) {
            case OFF -> Level.OFF;
            case FATAL -> Level.FATAL;
            case ERROR -> Level.ERROR;
            case WARN -> Level.WARN;
            case INFO -> Level.INFO;
            case DEBUG -> Level.DEBUG;
            case TRACE -> Level.TRACE;
            case ALL -> Level.ALL;
        };
    }

    private StatusModel runSimulation(IBuildAndRun func, SessionLogic logic, BaseSimulateRequestBody body, String sessionId) throws Exception {
        mapper.writeValue(new File(logic.rootDirectory, "simulate.json"), body);

        ErrorReporter reporter = new ErrorReporter();
        long preSimTime;
        long postSimTime;
        logic.setStatus(SessionLogic.SessionStatus.Simulating);

        logic.setStopRequested(false);
        preSimTime = System.currentTimeMillis();
        func.apply(new Maestro2Broker(logic.rootDirectory, reporter, logic::isStopRequested));
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
    }

    @FunctionalInterface
    private interface IBuildAndRun {
        void apply(Maestro2Broker broker) throws Exception;
    }

}