package org.intocps.orchestration.coe.webapi.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.core.config.Configurator;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.cosim.BasicFixedStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.util.Util;
import org.intocps.orchestration.coe.util.ZipDirectory;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/esav1/simulator")
public class EsaSimulationController {

    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(EsaSimulationController.class);
    final CoeService coeService;


    @Autowired
    public EsaSimulationController(CoeService coeService) {
        this.coeService = coeService;
    }


    @RequestMapping(value = "/ping")
    public String ping() {
        return "OK";
    }


    @RequestMapping(value = "/initialize", method = RequestMethod.POST)
    public void initializeSession(@RequestBody EsaIninializationData body) throws Exception, InitializationException {

        validate(body);

        logger.debug("Got initial data: {}", new ObjectMapper().writeValueAsString(body));
        mapper.writeValue(new File(coeService.get().getResultRoot(), "initialize.json"), body);

        if (body.simulatorLogLevel != null) {
//            LogManager.getRootLogger().setLevel(Level.toLevel(body.simulatorLogLevel.name()));
            Configurator.setLevel("root", org.apache.logging.log4j.Level.getLevel(body.simulatorLogLevel.name()));
        }

        CoSimStepSizeCalculator stepSizeCalculator = new BasicFixedStepSizeCalculator(body.stepSize);

        List<ModelConnection> connections = body.connections != null ? RequestProcessors.buildConnections(body.connections) : null;

        //Map<ModelConnection.ModelInstance, List<String>> logLevels = RequestProcessors.buildLogLevelsMap(body.getLogLevels());

        List<ModelParameter> parameters = RequestProcessors.buildParameters(body.parameters);

        Map<String, URI> fmus = body.getFmuFiles();

        List<ModelParameter> inputs = RequestProcessors.buildParameters(body.inputs);
        Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs = RequestProcessors.buildVariableMap(body.requestedOutputs);

        coeService.initialize(fmus, stepSizeCalculator, body.endTime, parameters, connections, body.getLogLevels(), inputs, outputs);
    }

    private void validate(EsaIninializationData body) throws InitializationException {

        if (body == null) {
            throw new InitializationException("Missing body");
        }

        if (body.fmus == null || body.fmus.isEmpty()) {
            throw new InitializationException("Missing fmus");
        }

        if (body.requestedOutputs == null || body.requestedOutputs.isEmpty()) {
            throw new InitializationException("Missing requested outputs");
        }

        if (body.stepSize == null) {
            throw new InitializationException("Missing step size");
        }

        if (body.stepSize < 0) {
            throw new InitializationException("Invalid step size " + body.stepSize + " must be > 0");
        }

        if (body.endTime == null) {
            throw new InitializationException("Missing end time");
        }

        if (body.endTime < 0) {
            throw new InitializationException("Invalid end time " + body.endTime + " must be > 0");
        }

        if (body.endTime < body.stepSize) {
            throw new InitializationException(
                    "End time must be equal or larger than step size. End time: " + body.endTime + ", Step size: " + body.stepSize);
        }

    }


    @RequestMapping(value = "/simulate", method = RequestMethod.POST)
    public Map<String, Map<String, Object>> simulate(@RequestBody
            EsaSimulateRequestBody body) throws CoeService.SimulatorNotConfigured, IOException, InvalidVariableStringException, ModelConnection.InvalidConnectionException {

        Coe coe = coeService.get();

        mapper.writeValue(new File(coe.getResultRoot(), "simulate.json"), body);

        List<ModelParameter> inputs = RequestProcessors.buildParameters(body.inputs);

        try {
            Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, Object>> outputs = coeService.simulate(body.timeStep, inputs);
            return outputs.entrySet().stream().collect(Collectors.toMap(x -> x.getKey().toString(),
                    x -> x.getValue().entrySet().stream().collect(Collectors.toMap(y -> y.getKey().name, Map.Entry::getValue))));
        } catch (Exception e) {
            logger.error("Error in simulation", e);
            throw e;
        }
    }

    @RequestMapping(value = "/stop", method = RequestMethod.POST)
    public void stop() {
        coeService.stop();
    }

    @RequestMapping(value = "/reset", method = RequestMethod.POST)
    public void reset() throws Exception {
        coeService.reset();
        coeService.reinitialize();
    }

    @RequestMapping(value = "/result/plain", method = RequestMethod.GET)
    public ResponseEntity<Resource> getResultPlain() throws Exception {
        Coe coe = coeService.get();
        if (coe == null) {
            throw new Exception("bad session");
        }

        File result = coe.getResult();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(result));

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getName() + "\"").contentLength(result.length())
                .body(resource);
    }

    @RequestMapping(value = "/result/zip", method = RequestMethod.GET, produces = "application/zip")
    public void getResultZip(HttpServletResponse response) throws Exception {
        Coe coe = coeService.get();
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
        Coe coe = coeService.get();
        if (coe == null) {
            throw new Exception("bad session");
        }

//        LoggerContext context = LoggerContext.getContext(false);
//        Configuration configuration = context.getConfiguration();
//
//
//        ArrayList<String> appendersToRemove = new ArrayList<>();
//        Map<String, Appender> appenders = configuration.getAppenders();
//
//        if (appenders != null) {
//
//            for(Map.Entry<String, Appender> appender :appenders.entrySet()) {
//                if (appender.getValue() instanceof FileAppender) {
//                    FileAppender fileAppender = (FileAppender) appender.getValue();
//                    //                    if (fileAppender.getFileName() != null && fileAppender.getFileName()
//                    //                            .matches("(.*)(" + sessionId + ")[/\\\\](.*)[/\\\\].*(\\.log)$")) {
//                    // Log files for fmu instances.
//                    // Regex matches <anything>+sessionId+</OR\>+<anything>+</OR\>+anything.log
//                    fileAppender.stop();
//                    appendersToRemove.add(fileAppender.getName());
//                    //                    }
//                }
//
//
//                appendersToRemove.forEach(fa -> {
//                    configuration.getRootLogger().removeAppender(fa);
//                });
//            }
//        }
      if(  Util.removeCoSimInstanceLogAppenders(null))

        FileUtils.deleteDirectory(coe.getResultRoot());
    }

    public static class SimulateResponse {
        @JsonProperty("requested_outputs")
        final Map<String, Map<String, Object>> requestedOutputs;

        @JsonCreator
        public SimulateResponse(@JsonProperty("requested_outputs") Map<String, Map<String, Object>> outputs) {
            this.requestedOutputs = outputs;
        }
    }

    public static class EsaSimulateRequestBody {
        @JsonProperty("time_step")
        final double timeStep;

        @JsonProperty("inputs")
        final Map<String, Object> inputs;


        @JsonCreator
        public EsaSimulateRequestBody(@JsonProperty("time_step") final double timeStep, @JsonProperty("inputs") final Map<String, Object> inputs) {
            this.timeStep = timeStep;
            this.inputs = inputs;
        }
    }


    public static class EsaIninializationData {
        @JsonProperty("fmus")
        private final Map<String, String> fmus;
        @JsonProperty("connections")
        private final Map<String, List<String>> connections;
        @JsonProperty("parameters")
        private final Map<String, Object> parameters;
        @JsonProperty("inputs")
        private final Map<String, Object> inputs;
        @JsonProperty("requested_outputs")
        private final Map<String, List<String>> requestedOutputs;
        @JsonProperty("step_size")
        private final Double stepSize;
        @JsonProperty("end_time")
        private final Double endTime;
        @JsonProperty("log_levels")
        private final Map<String, List<String>> logLevels;
        @JsonProperty("simulator_log_level")
        private final InitializeLogLevel simulatorLogLevel;

        @JsonCreator
        public EsaIninializationData(@JsonProperty("fmus") Map<String, String> fmus,
                @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
                @JsonProperty("inputs") final Map<String, Object> inputs, @JsonProperty("requested_outputs") Map<String, List<String>> outputs,
                @JsonProperty("step_size") Double stepSize, @JsonProperty("log_levels") final Map<String, List<String>> logLevels,
                @JsonProperty("end_time") final Double endTime, @JsonProperty("simulator_log_level") final InitializeLogLevel simulatorLogLevel) {
            this.fmus = fmus;
            this.connections = connections;
            this.parameters = parameters;
            this.inputs = inputs;
            this.requestedOutputs = outputs;
            this.stepSize = stepSize;
            this.endTime = endTime;
            this.logLevels = logLevels;
            this.simulatorLogLevel = simulatorLogLevel;
        }

        public Map<String, Object> getInputs() {
            return inputs;
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

        public Double getStepSize() {
            return stepSize;
        }

        public Double getEndTime() {
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
