package org.intocps.orchestration.coe.webapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.util.ZipDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/esav1/simulator")
public class EsaSimulationController {


    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(EsaSimulationController.class);

    Coe coe = null;


    @RequestMapping(value = "/ping")
    public String ping() {
        return "OK";
    }


    @RequestMapping(value = "/initialize", method = RequestMethod.POST)
    public void initializeSession(@RequestBody EsaIninializationData body) throws Exception {

        String session = UUID.randomUUID().toString();
        File root = new File(session);
        root.mkdirs();
        coe = new Coe(root);

        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body));

        if (coe == null) {
            throw new Exception("bad session");
        }

        if (body == null) {
            throw new Exception("Could not parse configuration: ");
        }

        if (body.simulatorLogLevel != null) {
            LogManager.getRootLogger().setLevel(Level.toLevel(body.simulatorLogLevel.name()));
        }

        if (body.fmus == null) {
            throw new Exception("FMUs must not be null");
        }

        if (body.connections == null) {
            throw new Exception("Connections must not be null");
        }

        //        CoSimStepSizeCalculator stepSizeCalculator = null;
        //        Algorithm stepAlgorithm = Algorithm.NONE;
        //        if (body.algorithm == null) {
        //
        //            stepAlgorithm = Algorithm.FIXED;
        //            stepSizeCalculator = new BasicFixedStepSizeCalculator(0.1);
        //            logger.info("No step size algorithm given. Defaulting to fixed-step with size 0.1");
        //
        //        } else if (body.algorithm instanceof FixedStepAlgorithmConfig) {
        //            FixedStepAlgorithmConfig algorithm = (FixedStepAlgorithmConfig) body.algorithm;
        //
        //            if (algorithm.size == null) {
        //                throw new Exception("fixed-step size must be an integer or double");
        //            }
        //
        //            logger.info("Using Fixed-step size calculator with size = {}", algorithm.size);
        //            stepSizeCalculator = new BasicFixedStepSizeCalculator(algorithm.size);
        //            stepAlgorithm = Algorithm.FIXED;
        //        } else if (body.algorithm instanceof VariableStepAlgorithmConfig) {
        //            VariableStepAlgorithmConfig algorithm = (VariableStepAlgorithmConfig) body.algorithm;
        //
        //            if (algorithm.size.length != 2) {
        //                logger.error("Unable to obtain the two size intervals");
        //                throw new Exception("size must be a 2-dimensional array of doubles: [minsize,maxsize]");
        //
        //            }
        //
        //            final StepsizeInterval stepsizeInterval = new StepsizeInterval(algorithm.size[0], algorithm.size[1]);
        //
        //            if (algorithm.initsize == null) {
        //                throw new Exception("initsize must be a double");
        //            }
        //
        //            if (algorithm.constraints != null) {
        //                for (IVarStepConstraint c : algorithm.constraints) {
        //                    c.validate();
        //                }
        //            }
        //
        //            Set<InitializationMsgJson.Constraint> constraints = algorithm.constraints == null ? null : algorithm.constraints.stream()
        //                    .map(c -> convert(c)).collect(Collectors.toSet());
        //            stepSizeCalculator = new VariableStepSizeCalculator(constraints, stepsizeInterval, algorithm.initsize);
        //            stepAlgorithm = Algorithm.VARSTEP;

        logger.info("Using Variable-step size calculator.");
        //        }


        Map<String, List<ModelDescription.LogCategory>> logs = null;

        //        mapper.writeValue(new File(coe.getResultRoot(), "initialize.json"), body);
        //        try {
        //            coe.getConfiguration().isStabalizationEnabled = body.stabalizationEnabled;
        //            coe.getConfiguration().global_absolute_tolerance = body.global_absolute_tolerance;
        //            coe.getConfiguration().global_relative_tolerance = body.global_relative_tolerance;
        //            coe.getConfiguration().loggingOn = body.loggingOn;
        //            coe.getConfiguration().visible = body.visible;
        //            coe.getConfiguration().parallelSimulation = body.parallelSimulation;
        //            coe.getConfiguration().simulationProgramDelay = body.simulationProgramDelay;
        //            coe.getConfiguration().hasExternalSignals = body.hasExternalSignals;
        //            logs = coe.initialize(body.getFmuFiles(), RequestProcessors.buildConnections(body.connections),
        //                    RequestProcessors.buildParameters(body.parameters), stepSizeCalculator,
        //                    new LogVariablesContainer(RequestProcessors.buildVariableMap(body.livestream),
        //                            RequestProcessors.buildVariableMap(body.logVariables)));
        //
        //
        //            if (stepAlgorithm == Algorithm.VARSTEP && !coe.canDoVariableStep()) {
        //                logger.error("Initialization failed: One or more FMUs cannot perform variable step size");
        //                //                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "One or more FMUs does not support variable step size.");
        //            }
        //            logger.trace("Initialization completed obtained the following logging categories: {}", logs);
        //
        //            return new InitializeStatusModel(coe.getState() + "", sessionId, null, coe.lastExecTime());
        //
        //        } catch (Exception e) {
        //            logger.error("Internal error in initialization", e);
        //            //            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
        //        }


        throw new Exception("internal error");
    }

    @RequestMapping(value = "/simulate", method = RequestMethod.POST)
    public void simulate(@RequestBody SimulateRequestBody body) throws Exception {


        mapper.writeValue(new File(coe.getResultRoot(), "simulate.json"), body);


        Map<ModelConnection.ModelInstance, List<String>> logLevels = new HashMap<>();

        if (body.logLevels != null) {
            for (Map.Entry<String, List<String>> entry : body.logLevels.entrySet()) {
                try {
                    logLevels.put(ModelConnection.ModelInstance.parse(entry.getKey()), entry.getValue());
                } catch (Exception e) {
                    throw new Exception("Error in logging " + "levels");
                }
            }
        }
        boolean async = false;
        if (!async) {
            try {
                coe.simulate(body.startTime, body.endTime, logLevels, body.reportProgress, body.liveLogInterval);
                //                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON,
                //                        mapper.writeValueAsString(sessionController.getStatus(sessionId)));
                return;
            } catch (Exception e) {
                logger.error("Error in simulation", e);
                throw e;
                //                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            (new Thread(() -> {
                try {
                    coe.simulate(body.startTime, body.endTime, logLevels, body.reportProgress, body.liveLogInterval);
                } catch (Exception e) {
                    coe.setLastError(e);
                }
            })).start();
            //            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON,
            //                    mapper.writeValueAsString(sessionController.getStatus(sessionId)));
            return;
        }
    }

    @RequestMapping(value = "/stops", method = RequestMethod.POST)
    public void stop() {
        if (coe != null) {
            coe.stopSimulation();
        }
    }

    @RequestMapping(value = "/result/plain", method = RequestMethod.GET)
    public ResponseEntity<Resource> getResultPlain() throws Exception {
        if (coe == null) {
            throw new Exception("bad session");
        }

        ByteArrayResource resource = new ByteArrayResource(FileUtils.readFileToByteArray(coe.getResult()));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + coe.getResult().getName() + "\"").body(resource);
    }

    @RequestMapping(value = "/result/zip", method = RequestMethod.GET, produces = "application/zip")
    public void getResultZip(HttpServletResponse response) throws Exception {
        if (coe == null) {
            throw new Exception("bad session");
        }

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"results.zip\"");

        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
        ZipDirectory.addDir(coe.getResultRoot(), coe.getResultRoot(), zipOutputStream);
        zipOutputStream.close();

    }

    @RequestMapping(value = "/destroy", method = RequestMethod.GET)
    public void destroy() throws Exception {
        if (coe == null) {
            throw new Exception("bad session");
        }

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        ArrayList<FileAppender> appendersToRemove = new ArrayList<>();
        Enumeration appenders = rootLogger.getAllAppenders();

        if (appenders != null) {
            while (appenders.hasMoreElements()) {
                try {
                    Object element = appenders.nextElement();
                    if (element != null && element instanceof FileAppender) {
                        FileAppender fileAppender = (FileAppender) element;
                        //                        if (fileAppender.getFile() != null && fileAppender.getFile()
                        //                                .matches("(.*)(" + sessionId + ")[/\\\\](.*)[/\\\\].*(\\.log)$")) {
                        //                            // Log files for fmu instances.
                        //                            // Regex matches <anything>+sessionId+</OR\>+<anything>+</OR\>+anything.log
                        //                            fileAppender.close();
                        //                            appendersToRemove.add(fileAppender);
                        //                        }
                    }
                } catch (NoSuchElementException e) {
                    //this is not synchronized so this can happen
                }
            }
            appendersToRemove.forEach(fa -> {
                rootLogger.removeAppender(fa);
            });

        }


        FileUtils.deleteDirectory(coe.getResultRoot());
    }


    public static class SimulateRequestBody {
        @JsonProperty("startTime")
        final double startTime;
        @JsonProperty("endTime")
        final double endTime;
        @JsonProperty("logLevels")
        final Map<String, List<String>> logLevels;
        @JsonProperty("reportProgress")
        final Boolean reportProgress;
        @JsonProperty("liveLogInterval")
        final Integer liveLogInterval;

        @JsonCreator
        public SimulateRequestBody(@JsonProperty("startTime") double startTime, @JsonProperty("endTime") double endTime,
                @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("reportProgress") Boolean reportProgress,
                @JsonProperty("liveLogInterval") Integer liveLogInterval) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.logLevels = logLevels;
            this.reportProgress = reportProgress;
            this.liveLogInterval = liveLogInterval;
        }
    }


    public static class EsaIninializationData {
        @JsonProperty("fmus")
        private final Map<String, String> fmus;
        @JsonProperty("connections")
        private final Map<String, List<String>> connections;
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;
        @JsonProperty("requested_outputs")
        private final Map<String, List<String>> requestedOutputs;
        @JsonProperty("step_size")
        private final double stepSize;
        @JsonProperty("end_time")
        private final double endTime;
        @JsonProperty("log_levels")
        private final Map<String, List<String>> logLevels;
        @JsonProperty("simulator_log_level")
        private final InitializeLogLevel simulatorLogLevel;


        @JsonCreator
        public EsaIninializationData(@JsonProperty("fmus") Map<String, String> fmus,
                @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
                @JsonProperty("requested_outputs") Map<String, List<String>> outputs, @JsonProperty("step_size") double stepSize,
                @JsonProperty("log_levels") final Map<String, List<String>> logLevels, @JsonProperty("end_time") final double endTime,
                @JsonProperty("simulator_log_level") final InitializeLogLevel simulatorLogLevel) {
            this.fmus = fmus;
            this.connections = connections;
            this.parameters = parameters;
            this.requestedOutputs = outputs;
            this.stepSize = stepSize;
            this.endTime = endTime;
            this.logLevels = logLevels;
            this.simulatorLogLevel = simulatorLogLevel;
        }

        public Map<String, List<String>> getConnections() {
            return connections;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public Map<String, List<String>> getRequestedOutputs() {
            return requestedOutputs;
        }

        public double getStepSize() {
            return stepSize;
        }

        public double getEndTime() {
            return endTime;
        }

        public Map<String, List<String>> getLogLevels() {
            return logLevels;
        }

        public InitializeLogLevel getSimulatorLogLevel() {
            return simulatorLogLevel;
        }

        @JsonIgnore
        public Map<String, URI> getFmuFiles() throws Exception {
            Map<String, URI> files = new HashMap<>();

            if (fmus != null) {
                for (Map.Entry<String, String> entry : fmus.entrySet()) {
                    try {
                        files.put(entry.getKey(), new URI(entry.getValue()));
                    } catch (Exception e) {
                        throw new Exception(entry.getKey() + "-" + entry.getValue() + ": " + e.getMessage(), e);
                    }
                }
            }

            return files;
        }

        public enum InitializeLogLevel {
            OFF,
            FATAL,
            ERROR,
            WARN,
            INFO,
            DEBUG,
            TRACE,
            ALL
        }
    }


}
