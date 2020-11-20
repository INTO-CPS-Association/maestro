package org.intocps.maestro.webapi.maestro2;

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
import org.intocps.maestro.Main;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.MableError;
import org.intocps.maestro.core.messages.MableWarning;
import org.intocps.maestro.webapi.controllers.ProdSessionLogicFactory;
import org.intocps.maestro.webapi.controllers.SessionController;
import org.intocps.maestro.webapi.controllers.SessionLogic;
import org.intocps.maestro.webapi.maestro2.dto.*;
import org.intocps.orchestration.coe.cosim.BasicFixedStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.httpserver.Algorithm;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.util.ZipDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;


@RestController
@Component
public class Maestro2SimulationController {

    public static final SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(Maestro2SimulationController.class);

    public static InitializationMsgJson.Constraint convert(IVarStepConstraint constraint) {
        if (constraint instanceof InitializationData.FmuMaxStepSizeConstraint) {
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "fmumaxstepsize";
            return c;

        } else if (constraint instanceof InitializationData.BoundedDifferenceConstraint) {
            InitializationData.BoundedDifferenceConstraint cIn = (InitializationData.BoundedDifferenceConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "boundeddifference";
            c.abstol = cIn.getAbstol();
            c.ports = cIn.getPorts();
            c.reltol = cIn.getReltol();
            c.safety = cIn.getSafety();
            c.skipDiscrete = cIn.getSkipDiscrete();
            return c;

        } else if (constraint instanceof InitializationData.SamplingConstraint) {
            InitializationData.SamplingConstraint cIn = (InitializationData.SamplingConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "samplingrate";
            c.base = cIn.getBase();
            c.rate = cIn.getRate();
            c.startTime = cIn.getStartTime();
            return c;

        } else if (constraint instanceof InitializationData.ZeroCrossingConstraint) {
            InitializationData.ZeroCrossingConstraint cIn = (InitializationData.ZeroCrossingConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "zerocrossing";
            c.abstol = cIn.getAbstol();
            c.ports = cIn.getPorts();
            c.order = cIn.getOrder();
            c.safety = cIn.getSafety();
            return c;
        }
        return null;
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
            @ApiParam(value = "File", required = true) @RequestParam("fieldFile") MultipartFile file) throws IOException {
        throw new NotImplementedException("/upload/{sessionId} has not been implemented.");
        //        try (InputStream is = file.getInputStream()) {
        //            logger.debug("Uploaded file: {}", file.getOriginalFilename());
        //            File targetFile = new File(sessions.get(sessionId).getResultRoot(), file.getOriginalFilename());
        //            IOUtils.copy(is, new FileOutputStream(targetFile));
        //        }

    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String ping() {
        return "OK";
    }

    @RequestMapping(value = "/status/{sessionId}", method = RequestMethod.GET)
    public List<StatusModel> getStatuses(@PathVariable String sessionId) {
        throw new NotImplementedException("/status/{sessionId} has not been implemented.");
    }

    StatusModel getStatus(String sessionId) {
        if (sessionController.containsSession(sessionId)) {
            return new StatusModel("Session exists", sessionId, 0);
        } else {
            return new StatusModel("Session does not exist", sessionId, 0);
        }
    }

    @RequestMapping(value = "/createSession", method = RequestMethod.GET)
    public StatusModel createSession() {
        String session = sessionController.createNewSession();
        return getStatus(session);
    }

    @RequestMapping(value = "/initialize/{sessionId}", method = RequestMethod.POST)
    public InitializeStatusModel initializeSession(@PathVariable String sessionId, @RequestBody String body1) throws Exception {
        // Store this data to be used for the interpretor later on.
        // It is not possible to create the spec at this point in time as data for setup experiment is missing (i.e. endtime)
        //        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body1));
        logger.debug("Got initial data: {}", body1);
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        mapper.writeValue(new File(logic.rootDirectory, "initialize.json"), body1);
        ObjectMapper mapper = new ObjectMapper();//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InitializationData body = mapper.readValue(body1, InitializationData.class);


        if (logic == null) {
            throw new Exception("Session has not been created.");
        }

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

        CoSimStepSizeCalculator stepSizeCalculator = null;
        Algorithm stepAlgorithm = Algorithm.NONE;
        if (body.getAlgorithm() == null) {

            stepAlgorithm = Algorithm.FIXED;
            stepSizeCalculator = new BasicFixedStepSizeCalculator(0.1);
            logger.info("No step size algorithm given. Defaulting to fixed-step with size 0.1");

        } else if (body.getAlgorithm() instanceof FixedStepAlgorithmConfig) {
            FixedStepAlgorithmConfig algorithm = (FixedStepAlgorithmConfig) body.getAlgorithm();

            if (algorithm.size == null) {
                throw new Exception("fixed-step size must be an integer or double");
            }

            logger.info("Using Fixed-step size calculator with size = {}", algorithm.size);
            stepSizeCalculator = new BasicFixedStepSizeCalculator(algorithm.size);
            stepAlgorithm = Algorithm.FIXED;
        } else if (body.getAlgorithm() instanceof VariableStepAlgorithmConfig) {
            logger.info("Variable step algorithm not supported");
            throw new NotImplementedException("Variable step algorithms are not supported.");
        }
        Map<String, List<ModelDescription.LogCategory>> logs = null;

        if (body.isStabalizationEnabled()) {

            //            if (body.global_absolute_tolerance != 0.0) {
            //                throw new NotImplementedException("global absolute tolerance is not implemented");
            //            }
            //            if (body.global_relative_tolerance != 0.0) {
            //                throw new NotImplementedException("global absolute tolerance is not implemented");
            //            }
            throw new NotImplementedException("Stabilisation is not implemented");
        }
        if (body.isParallelSimulation()) {
            throw new NotImplementedException("ParallelSimulation is not implemented");
        }
        if (body.isSimulationProgramDelay()) {
            throw new NotImplementedException("SimulationProgramDelay is not implemented");
        }

        if (body.isHasExternalSignals()) {
            throw new NotImplementedException("HasExternalSignals is not implemented");
        }


        logger.trace("Initialization completed");
        logic.setInitializationData(body);


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
    @RequestMapping(value = "/simulate/{sessionId}", method = RequestMethod.POST, consumes = {"text/plain", "application/json"})
    public StatusModel simulate(@PathVariable String sessionId, @RequestBody SimulateRequestBody body) throws Exception {
        SessionLogic logic = sessionController.getSessionLogic(sessionId);
        mapper.writeValue(new File(logic.rootDirectory, "simulate.json"), body);

        ErrorReporter reporter = new ErrorReporter();
        Maestro2Broker mc = new Maestro2Broker(logic.rootDirectory, reporter);
        mc.buildAndRun(logic.getInitializationData(), body, logic.getSocket(), new File(logic.rootDirectory, "outputs.csv"));

        if (reporter.getErrorCount() > 0) {
            reporter.getErrors().forEach(x -> logger.error(x.toString()));
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            reporter.printWarnings(writer);
            reporter.printErrors(writer);
            throw new Exception(out.toString());
        }

        reporter.getWarnings().forEach(x -> logger.warn(x.toString()));

        return new StatusModel("Simulation completed", sessionId, 0,
                reporter.getErrors().stream().map(MableError::toString).collect(Collectors.toList()),
                reporter.getWarnings().stream().map(MableWarning::toString).collect(Collectors.toList()));
    }


    @RequestMapping(value = "/stopsimulation/{sessionId}", method = RequestMethod.POST)
    public void stop(@PathVariable String sessionId) {
        throw new NotImplementedException("/stopsimulation/{sessionId} has not been implemented.");
        //        if (sessions.containsKey(sessionId)) {
        //            sessions.get(sessionId).stopSimulation();
        //        }
    }

    @RequestMapping(value = "/result/{sessionId}/plain", method = RequestMethod.GET)
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

    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public String version() {
        final String message = "{\"version\":\"" + Main.getVersion() + "\"}";
        return message;
    }

    //    @RequestMapping(value = "/reset/{sessionId}", method = RequestMethod.GET)
    //    public void reset(@PathVariable String sessionId) {
    //
    //    }


}