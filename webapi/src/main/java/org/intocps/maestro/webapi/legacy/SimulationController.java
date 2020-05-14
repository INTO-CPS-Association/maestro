package org.intocps.maestro.webapi.legacy;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.FileAppender;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.BasicFixedStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.VariableStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.httpserver.Algorithm;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.LogVariablesContainer;
import org.intocps.orchestration.coe.util.ZipDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/legacy/")
public class SimulationController {
    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(SimulationController.class);
    private final Map<String, Coe> sessions = new HashMap<>();

    public static InitializationMsgJson.Constraint convert(IVarStepConstraint constraint) {
        if (constraint instanceof IninializationData.FmuMaxStepSizeConstraint) {
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "fmumaxstepsize";
            return c;

        } else if (constraint instanceof IninializationData.BoundedDifferenceConstraint) {
            IninializationData.BoundedDifferenceConstraint cIn = (IninializationData.BoundedDifferenceConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "boundeddifference";
            c.abstol = cIn.abstol;
            c.ports = cIn.ports;
            c.reltol = cIn.reltol;
            c.safety = cIn.safety;
            c.skipDiscrete = cIn.skipDiscrete;
            return c;

        } else if (constraint instanceof IninializationData.SamplingConstraint) {
            IninializationData.SamplingConstraint cIn = (IninializationData.SamplingConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "samplingrate";
            c.base = cIn.base;
            c.rate = cIn.rate;
            c.startTime = cIn.startTime;
            return c;

        } else if (constraint instanceof IninializationData.ZeroCrossingConstraint) {
            IninializationData.ZeroCrossingConstraint cIn = (IninializationData.ZeroCrossingConstraint) constraint;
            InitializationMsgJson.Constraint c = new InitializationMsgJson.Constraint();
            c.type = "zerocrossing";
            c.abstol = cIn.abstol;
            c.ports = cIn.ports;
            c.order = cIn.order;
            c.safety = cIn.safety;
            return c;
        }
        return null;
    }

    @RequestMapping(value = "/upload/{sessionId}", method = RequestMethod.POST)
    public void uploadFile(@PathVariable String sessionId,
            @ApiParam(value = "File", required = true) @RequestParam("fieldFile") MultipartFile file) throws IOException {

        try (InputStream is = file.getInputStream()) {
            logger.debug("Uploaded file: {}", file.getOriginalFilename());
            File targetFile = new File(sessions.get(sessionId).getResultRoot(), file.getOriginalFilename());
            IOUtils.copy(is, new FileOutputStream(targetFile));
        }

    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String ping() {
        return "OK";
    }

    StatusModel getStatus(String sessionId) {

        Coe coe = sessions.get(sessionId);
        return new StatusModel(coe.getState() + "", sessionId, coe.getLastExecTime());
    }

    @RequestMapping(value = "/status/{sessionId}", method = RequestMethod.GET)
    public List<StatusModel> getStatuses(@PathVariable String sessionId) {
        return Arrays.asList(getStatus(sessionId));
    }

    @RequestMapping(value = "/createSession", method = RequestMethod.GET)
    public StatusModel createSession() {

        String session = UUID.randomUUID().toString();

        File root = new File(session);
        root.mkdirs();
        sessions.put(session, new Coe(root));
        return getStatus(session);
    }

    @RequestMapping(value = "/initialize/{sessionId}", method = RequestMethod.POST)
    public InitializeStatusModel initializeSession(@PathVariable String sessionId, @RequestBody IninializationData body) throws Exception {

        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body));

        Coe coe = sessions.get(sessionId);
        if (coe == null) {
            throw new Exception("bad session");
        }

        if (body == null) {
            throw new Exception("Could not parse configuration: ");
        }

        //        if (body.overrideLogLevel != null)
        //        {
        //            LogManager.getRootLogger().setLevel(Level.toLevel(body.overrideLogLevel));
        //        }

        if (body.fmus == null) {
            throw new Exception("FMUs must not be null");
        }

        if (body.connections == null) {
            throw new Exception("Connections must not be null");
        }

        CoSimStepSizeCalculator stepSizeCalculator = null;
        Algorithm stepAlgorithm = Algorithm.NONE;
        if (body.algorithm == null) {

            stepAlgorithm = Algorithm.FIXED;
            stepSizeCalculator = new BasicFixedStepSizeCalculator(0.1);
            logger.info("No step size algorithm given. Defaulting to fixed-step with size 0.1");

        } else if (body.algorithm instanceof FixedStepAlgorithmConfig) {
            FixedStepAlgorithmConfig algorithm = (FixedStepAlgorithmConfig) body.algorithm;

            if (algorithm.size == null) {
                throw new Exception("fixed-step size must be an integer or double");
            }

            logger.info("Using Fixed-step size calculator with size = {}", algorithm.size);
            stepSizeCalculator = new BasicFixedStepSizeCalculator(algorithm.size);
            stepAlgorithm = Algorithm.FIXED;
        } else if (body.algorithm instanceof VariableStepAlgorithmConfig) {
            VariableStepAlgorithmConfig algorithm = (VariableStepAlgorithmConfig) body.algorithm;

            if (algorithm.size.length != 2) {
                logger.error("Unable to obtain the two size intervals");
                throw new Exception("size must be a 2-dimensional array of doubles: [minsize,maxsize]");

            }

            final StepsizeInterval stepsizeInterval = new StepsizeInterval(algorithm.size[0], algorithm.size[1]);

            if (algorithm.initsize == null) {
                throw new Exception("initsize must be a double");
            }

            if (algorithm.constraints != null) {
                for (IVarStepConstraint c : algorithm.constraints) {
                    c.validate();
                }
            }

            Set<InitializationMsgJson.Constraint> constraints = algorithm.constraints == null ? null : algorithm.constraints.stream()
                    .map(c -> convert(c)).collect(Collectors.toSet());
            stepSizeCalculator = new VariableStepSizeCalculator(constraints, stepsizeInterval, algorithm.initsize);
            stepAlgorithm = Algorithm.VARSTEP;

            logger.info("Using Variable-step size calculator.");
        }


        Map<String, List<ModelDescription.LogCategory>> logs = null;

        mapper.writeValue(new File(coe.getResultRoot(), "initialize.json"), body);
        try {
            coe.getConfiguration().isStabalizationEnabled = body.stabalizationEnabled;
            coe.getConfiguration().global_absolute_tolerance = body.global_absolute_tolerance;
            coe.getConfiguration().global_relative_tolerance = body.global_relative_tolerance;
            coe.getConfiguration().loggingOn = body.loggingOn;
            coe.getConfiguration().visible = body.visible;
            coe.getConfiguration().parallelSimulation = body.parallelSimulation;
            coe.getConfiguration().simulationProgramDelay = body.simulationProgramDelay;
            coe.getConfiguration().hasExternalSignals = body.hasExternalSignals;
            logs = coe.initialize(body.getFmuFiles(), RequestProcessors.buildConnections(body.connections),
                    RequestProcessors.buildParameters(body.parameters), stepSizeCalculator,
                    new LogVariablesContainer(RequestProcessors.buildVariableMap(body.livestream),
                            RequestProcessors.buildVariableMap(body.logVariables)));


            if (stepAlgorithm == Algorithm.VARSTEP && !coe.canDoVariableStep()) {
                logger.error("Initialization failed: One or more FMUs cannot perform variable step size");
                //                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "One or more FMUs does not support variable step size.");
            }
            logger.trace("Initialization completed obtained the following logging categories: {}", logs);

            return new InitializeStatusModel(coe.getState() + "", sessionId, null, coe.lastExecTime());

        } catch (Exception e) {
            logger.error("Internal error in initialization", e);
            //            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
        }


        throw new Exception("internal error");
    }

    @RequestMapping(value = "/simulate/{sessionId}", method = RequestMethod.POST)
    public StatusModel simulate(@PathVariable String sessionId, @RequestBody SimulateRequestBody body) throws Exception {

        Coe coe = sessions.get(sessionId);

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
                return getStatus(sessionId);
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
            return getStatus(sessionId);
        }
    }

    @RequestMapping(value = "/stopsimulation/{sessionId}", method = RequestMethod.POST)
    public void stop(@PathVariable String sessionId) {
        if (sessions.containsKey(sessionId)) {
            sessions.get(sessionId).stopSimulation();
        }
    }

    @RequestMapping(value = "/result/{sessionId}/plain", method = RequestMethod.GET)
    public ResponseEntity<Resource> getResultPlain(@PathVariable String sessionId) throws Exception {
        Coe coe = sessions.get(sessionId);
        if (coe == null) {
            throw new Exception("bad session");
        }

        ByteArrayResource resource = new ByteArrayResource(FileUtils.readFileToByteArray(coe.getResult()));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + coe.getResult().getName() + "\"").body(resource);
    }

    @RequestMapping(value = "/result/{sessionId}/zip", method = RequestMethod.GET, produces = "application/zip")
    public void getResultZip(@PathVariable String sessionId, HttpServletResponse response) throws Exception {
        Coe coe = sessions.get(sessionId);
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

    @RequestMapping(value = "/destroy/{sessionId}", method = RequestMethod.GET)
    public void destroy(@PathVariable String sessionId) throws Exception {
        Coe coe = sessions.get(sessionId);
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
                        if (fileAppender.getFile() != null && fileAppender.getFile()
                                .matches("(.*)(" + sessionId + ")[/\\\\](.*)[/\\\\].*(\\.log)$")) {
                            // Log files for fmu instances.
                            // Regex matches <anything>+sessionId+</OR\>+<anything>+</OR\>+anything.log
                            fileAppender.close();
                            appendersToRemove.add(fileAppender);
                        }
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

    //    @RequestMapping(value = "/reset/{sessionId}", method = RequestMethod.GET)
    //    public void reset(@PathVariable String sessionId) {
    //
    //    }

    @ApiModel(subTypes = {FixedStepAlgorithmConfig.class, VariableStepAlgorithmConfig.class}, discriminator = "type",
            description = "Simulation algorithm.")

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
            {@Type(value = FixedStepAlgorithmConfig.class, name = "fixed-step"), @Type(value = VariableStepAlgorithmConfig.class, name = "var-step")})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public interface IAlgorithmConfig {
    }


    @ApiModel(subTypes = {IninializationData.BoundedDifferenceConstraint.class, IninializationData.ZeroCrossingConstraint.class,
            IninializationData.SamplingConstraint.class, IninializationData.FmuMaxStepSizeConstraint.class}, discriminator = "type",
            description = "Simulation variable step algorithm constraint.", value = "VarStepConstraint")

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY, visible = true)
    @JsonSubTypes({@Type(value = IninializationData.BoundedDifferenceConstraint.class, name = "boundeddifference"),
            @Type(value = IninializationData.ZeroCrossingConstraint.class, name = "zerocrossing"),
            @Type(value = IninializationData.SamplingConstraint.class, name = "samplingrate"),
            @Type(value = IninializationData.FmuMaxStepSizeConstraint.class, name = "fmumaxstepsize")})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public interface IVarStepConstraint {

        void validate() throws Exception;
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

    public static class StatusModel {
        @JsonProperty("status")
        final String status;
        @JsonProperty("sessionid")
        final String sessionId;

        @JsonProperty("lastExecTime")
        final long lastExecTime;

        public StatusModel(String status, String sessionId, long lastExecTime) {
            this.status = status;
            this.sessionId = sessionId;
            this.lastExecTime = lastExecTime;
        }
    }

    public static class InitializeStatusModel extends StatusModel {

        @JsonProperty("avaliableLogLevels")
        private final Map<String, List<LogLevelModel>> avaliableLogLevels;

        @JsonCreator
        public InitializeStatusModel(@JsonProperty("status") String status, @JsonProperty("sessionid") String sessionId,
                @JsonProperty("avaliableLogLevels") Map<String, List<LogLevelModel>> avaliableLogLevels,
                @JsonProperty("lastExecTime") final long lastExecTime) {
            super(status, sessionId, lastExecTime);
            this.avaliableLogLevels = avaliableLogLevels;
        }

        public static class LogLevelModel {
            final String name;
            final String description;

            public LogLevelModel(String name, String description) {
                this.name = name;
                this.description = description;
            }
        }
    }

    @ApiModel(parent = IAlgorithmConfig.class)
    public static class FixedStepAlgorithmConfig implements IAlgorithmConfig {
        @JsonProperty("size")
        public final Double size;

        @JsonCreator
        public FixedStepAlgorithmConfig(@JsonProperty("size") Double size) {
            this.size = size;
        }

        public Double getSize() {
            return size;
        }
    }

    @ApiModel(parent = IAlgorithmConfig.class)
    public static class VariableStepAlgorithmConfig implements IAlgorithmConfig {

        @JsonProperty("size")
        final Double[] size;
        @JsonProperty("initsize")
        final Double initsize;
        @JsonProperty("constraints")
        final List<IVarStepConstraint> constraints;

        public VariableStepAlgorithmConfig(@JsonProperty("size") Double[] size, @JsonProperty("initsize") Double initsize,
                @JsonProperty("constraints") final List<IVarStepConstraint> constraints) {
            this.size = size;
            this.initsize = initsize;
            this.constraints = constraints;
        }

        public Double[] getSize() {
            return size;
        }

        public Double getInitsize() {
            return initsize;
        }

        public List<IVarStepConstraint> getConstraints() {
            return constraints;
        }

    }

    public static class IninializationData {
        @JsonProperty("fmus")
        final Map<String, String> fmus;
        @JsonProperty("connections")
        final Map<String, List<String>> connections;
        @JsonProperty("parameters")
        final Map<String, Object> parameters;
        @JsonProperty("livestream")
        final Map<String, List<String>> livestream;
        @JsonProperty("logvariables")
        final Map<String, List<String>> logVariables;
        @JsonProperty("parallelSimulation")
        final boolean parallelSimulation;
        @JsonProperty("stabalizationEnabled")
        final boolean stabalizationEnabled;
        @JsonProperty("global_absolute_tolerance")
        final double global_absolute_tolerance;
        @JsonProperty("global_relative_tolerance")
        final double global_relative_tolerance;
        @JsonProperty("loggingOn")
        final boolean loggingOn;
        @JsonProperty("visible")
        final boolean visible;
        @JsonProperty("simulationProgramDelay")
        final boolean simulationProgramDelay;
        @JsonProperty("hasExternalSignals")
        final boolean hasExternalSignals;
        @JsonProperty("overrideLogLevel")
        final InitializeLogLevel overrideLogLevel;
        @JsonProperty("algorithm")
        IAlgorithmConfig algorithm;

        @JsonCreator
        public IninializationData(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
                @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("livestream") Map<String, List<String>> livestream,
                @JsonProperty("logvariables") Map<String, List<String>> logVariables, @JsonProperty("parallelSimulation") boolean parallelSimulation,
                @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
                @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
                @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
                @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
                @JsonProperty("hasExternalSignals") boolean hasExternalSignals, @JsonProperty("algorithm") IAlgorithmConfig algorithm,
                @JsonProperty("overrideLogLevel") final InitializeLogLevel overrideLogLevel) {
            this.fmus = fmus;
            this.connections = connections;
            this.parameters = parameters;
            this.livestream = livestream;
            this.logVariables = logVariables;
            this.loggingOn = loggingOn;
            this.visible = visible;
            this.simulationProgramDelay = simulationProgramDelay;
            this.hasExternalSignals = hasExternalSignals;
            this.parallelSimulation = parallelSimulation;
            this.stabalizationEnabled = stabalizationEnabled;
            this.global_absolute_tolerance = global_absolute_tolerance;
            this.global_relative_tolerance = global_relative_tolerance;
            this.algorithm = algorithm;
            this.overrideLogLevel = overrideLogLevel;
        }

        public InitializeLogLevel getOverrideLogLevel() {
            return overrideLogLevel;
        }

        public Map<String, String> getFmus() {
            return fmus;
        }

        public Map<String, List<String>> getConnections() {
            return connections;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public Map<String, List<String>> getLivestream() {
            return livestream;
        }

        public Map<String, List<String>> getLogVariables() {
            return logVariables;
        }

        public boolean isParallelSimulation() {
            return parallelSimulation;
        }

        public boolean isStabalizationEnabled() {
            return stabalizationEnabled;
        }

        public double getGlobal_absolute_tolerance() {
            return global_absolute_tolerance;
        }

        public double getGlobal_relative_tolerance() {
            return global_relative_tolerance;
        }

        public boolean isLoggingOn() {
            return loggingOn;
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isSimulationProgramDelay() {
            return simulationProgramDelay;
        }

        public boolean isHasExternalSignals() {
            return hasExternalSignals;
        }

        public IAlgorithmConfig getAlgorithm() {
            return algorithm;
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

        enum InitializeLogLevel {
            OFF,
            FATAL,
            ERROR,
            WARN,
            INFO,
            DEBUG,
            TRACE,
            ALL
        }

        @ApiModel(parent = IVarStepConstraint.class)
        public static class SamplingConstraint implements IVarStepConstraint {
            final Integer base;
            final Integer rate;
            final Integer startTime;

            public SamplingConstraint(Integer base, Integer rate, Integer startTime) {
                this.base = base;
                this.rate = rate;
                this.startTime = startTime;
            }

            public Integer getBase() {
                return base;
            }

            public Integer getRate() {
                return rate;
            }

            public Integer getStartTime() {
                return startTime;
            }

            @Override
            public void validate() throws Exception {

            }
        }

        @ApiModel(parent = IVarStepConstraint.class)
        public static class FmuMaxStepSizeConstraint implements IVarStepConstraint {

            @Override
            public void validate() throws Exception {

            }
        }

        @ApiModel(parent = IVarStepConstraint.class)
        public static class BoundedDifferenceConstraint implements IVarStepConstraint {
            final List<String> ports;
            final Double reltol;
            final Double abstol;
            final Double safety;
            final Boolean skipDiscrete;

            public BoundedDifferenceConstraint(List<String> ports, Double reltol, Double abstol, Double safety, Boolean skipDiscrete) {
                this.ports = ports;
                this.reltol = reltol;
                this.abstol = abstol;
                this.safety = safety;
                this.skipDiscrete = skipDiscrete;
            }

            public List<String> getPorts() {
                return ports;
            }

            public Double getReltol() {
                return reltol;
            }

            public Double getAbstol() {
                return abstol;
            }

            public Double getSafety() {
                return safety;
            }

            public Boolean getSkipDiscrete() {
                return skipDiscrete;
            }

            @Override
            public void validate() throws Exception {

            }
        }

        @ApiModel(parent = IVarStepConstraint.class)
        public static class ZeroCrossingConstraint implements IVarStepConstraint {
            final List<String> ports;
            final Integer order;
            final Double abstol;
            final Double safety;

            public ZeroCrossingConstraint(List<String> ports, Integer order, Double abstol, Double safety) {
                this.ports = ports;
                this.order = order;
                this.abstol = abstol;
                this.safety = safety;
            }

            public List<String> getPorts() {
                return ports;
            }

            public Integer getOrder() {
                return order;
            }

            public Double getAbstol() {
                return abstol;
            }

            public Double getSafety() {
                return safety;
            }

            @Override
            public void validate() throws Exception {

            }
        }

        //    @RequestMapping(value = "", method = RequestMethod.POST)
        //    public void createField(@RequestBody FieldRequest fieldRequest, Principal principal) throws Exception {
        //        int tenantId = tenantDataService.getTenantId(principal.getName());
        //        logger.debug("Creating field, user {}, tenant id {}", principal.getName(), tenantId);
        //        com.agcocorp.logistics.resources.model.FieldConfiguration mapped = buildField(fieldRequest);
        //        com.agcocorp.logistics.resources.model.Field created = service.create(tenantId, mapped);
        //        return modelMapperService.getModelMapper().map(created);
        //    }
    }
}