/*
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-CurrentYear, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License
 * are obtained from the INTO-CPS Association, either from the above address,
 * from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

/*
 * Author:
 *		Kenneth Lausdahl
 *		Casper Thule
 */
package org.intocps.orchestration.coe.httpserver;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.cosim.BasicFixedStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.VariableStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.intocps.orchestration.coe.json.InitializationStatusJson;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.CoeStatus;
import org.intocps.orchestration.coe.scala.LogVariablesContainer;
import org.intocps.orchestration.coe.util.DeleteOnCloseFileInputStream;
import org.intocps.orchestration.coe.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static org.intocps.orchestration.coe.util.ZipDirectory.zipDirectoryLarge;

/**
 * Created by ctha on 17-03-2016.
 */
public class RequestProcessors {

    final static Logger logger = LoggerFactory.getLogger(RequestProcessors.class);
    final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    private final SessionController sessionController;

    public RequestProcessors(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public static List<ModelParameter> buildParameters(Map<String, Object> parameters) throws InvalidVariableStringException {
        List<ModelParameter> list = new Vector<>();

        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                list.add(new ModelParameter(ModelConnection.Variable.parse(entry.getKey()), entry.getValue()));
            }
        }
        return list;
    }

    public static Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> buildVariableMap(
            Map<String, List<String>> rawVariableMap) throws Exception {
        Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> map = null;
        if (rawVariableMap != null) {
            map = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : rawVariableMap.entrySet()) {
                ModelConnection.ModelInstance instance = ModelConnection.ModelInstance.parse(entry.getKey());
                Set<ModelDescription.ScalarVariable> scalars = new HashSet<>();
                for (String scalarEntry : entry.getValue()) {
                    ModelDescription.ScalarVariable variable = new ModelDescription.ScalarVariable();
                    variable.name = scalarEntry;
                    scalars.add(variable);
                }
                map.put(instance, scalars);
            }
        }
        return map;
    }

    public static List<ModelConnection> buildConnections(Map<String, List<String>> connections) throws Exception {
        List<ModelConnection> list = new Vector<>();

        for (Map.Entry<String, List<String>> entry : connections.entrySet()) {
            for (String input : entry.getValue()) {
                list.add(new ModelConnection(ModelConnection.Variable.parse(entry.getKey()), ModelConnection.Variable.parse(input)));
            }
        }

        return list;
    }

    public NanoHTTPD.Response processCreateSession() {
        String session = this.sessionController.createNewSession();
        //return new Response(Response.HTTP_OK, Response.MIME_JSON, String.format("{\"sessionId\":\"%d\"}", session));
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, String.format("{\"sessionId\":\"%s\"}", session));
    }

    // cURL test with 3 forms and 3 files: curl -v -i -F file1=@modelDescription.rar -F file2=@target\tank.fmu -F
    // file3=@target\tank2.fmu localhost:8082/upload
    public NanoHTTPD.Response processFileUpload(String sessionId, Map<String, String> headers, InputStream inputStream) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        String contentType = headers.get("content-type");
        if (contentType != null && contentType.indexOf("multipart/form-data") != -1) {
            // Get the boundary
            String boundary = contentType.split("boundary=")[1];

            // "\r\n--" is added because the usage of boundary differs from its definition in the content-type header.
            byte[] boundaryArr = ("\r\n--" + boundary).getBytes();

            try {
                String startBoundary = ProcessingUtils.readUntilLineEnd(bufferedInputStream);

                if (startBoundary.indexOf("--" + boundary) != -1) {
                    String restData = "";
                    String filename;
                    do {
                        // Extract headers for the part
                        String partHeaders = ProcessingUtils.readUntilBlank(bufferedInputStream);
                        if (partHeaders.contains("filename=")) {
                            // Extract filename
                            String[] filelist = partHeaders.split("filename=\"")[1].split("\"")[0]
                                    .split("\\" + System.getProperty("file.separator")); // Necessary with both?
                            filename = filelist[filelist.length - 1];
                            // Process the binary data in the part
                            String filePath = sessionController.getSessionRootDir(sessionId) + "/" + filename;
                            filePath = filePath.replace('/', File.separatorChar);
                            ProcessingUtils.storePartContent(new File(filePath), bufferedInputStream, boundaryArr);
                            // Read till end of boundary. Might just be "\r\n".
                            restData = ProcessingUtils.readUntilLineEnd(bufferedInputStream);
                            // System.out.println(restData);
                        }
                    } while (!restData.equals("--")); // If -- comes right after the boundary, it marks the end of the multipart
                    return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, "Files uploaded..");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
            }
        }
        return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Invalid content-type " + contentType);
    }

    @SuppressWarnings("unchecked")
    public NanoHTTPD.Response processInitialize(String sessionId, String data) throws IOException, NanoHTTPD.ResponseException {
        if (data == null || data.isEmpty()) {
            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "missing json data for configuration");
            //return new Response(Response.HTTP_BADREQUEST, Response.MIME_PLAINTEXT, "missing json data for configuration");
        }

        try {
            InitializationMsgJson st = null;

            try {
                st = mapper.readValue(data, InitializationMsgJson.class);
            } catch (JsonMappingException e) {
                logger.error("Unable to parse initialization json", e);
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, e.getMessage());
            }

            if (st == null) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Could not parse configuration: " + data);
            }

            if (st.overrideLogLevel != null) {
                org.intocps.orchestration.coe.util.Util.setLogLevel(st.overrideLogLevel);

            }

            if (st.getFmuFiles() == null) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "FMUs must not be null");
            }

            if (st.connections == null) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Connections must not be null");
            }

            CoSimStepSizeCalculator stepSizeCalculator = null;
            Algorithm algorithm = Algorithm.NONE;
            if (st.algorithm != null) {
                if ("fixed-step".equals(st.algorithm.get("type"))) {

                    Object size = st.algorithm.get("size");
                    if (!ProcessingUtils.canObjectBeCastToDouble(size)) {
                        logger.error("Fixed step size cannot be cast to double");
                        return ProcessingUtils
                                .newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "fixed-step size must be an integer or double");
                    }
                    Double sizeD = ProcessingUtils.castObjectToDouble(size);

                    logger.info("Using Fixed-step size calculator with size = {}", sizeD);
                    stepSizeCalculator = new BasicFixedStepSizeCalculator(sizeD);
                    algorithm = Algorithm.FIXED;
                } else if ("var-step".equals(st.algorithm.get("type"))) {
                    List<Double> size = new Vector<>();
                    Object objSize = st.algorithm.get("size");

                    if (size instanceof List) {
                        for (Object number : (List<Object>) objSize) {
                            if (number instanceof Integer) {
                                number = new Double((Integer) number);
                            }

                            if (number instanceof Double) {
                                size.add((Double) number);
                            } else {

                                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                                        "size must be a 2-dimensional array of doubles: [minsize,maxsize]. One of the sizes " + number + " was not a double.");
                            }
                        }
                    } else {
                        logger.error("Unable to obtain the two size intervals. Not a list.");
                        return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                                "size must be a 2-dimensional array of doubles: [minsize,maxsize]. No list found.");
                    }

                    if (size.size() != 2) {
                        logger.error("Unable to obtain the two size intervals");
                        return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                                "size must be a 2-dimensional array of doubles: [minsize,maxsize]");

                    }

                    final StepsizeInterval stepsizeInterval = new StepsizeInterval(size.get(0), size.get(1));

                    Double initsize;
                    Object objInitsize = st.algorithm.get("initsize");
                    if (objInitsize instanceof Double) {
                        initsize = (Double) objInitsize;
                    } else if (objInitsize instanceof Integer) {
                        initsize = new Double((Integer) objInitsize);
                    } else {
                        logger.error("initsize is not a double");
                        return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "initsize must be a double");
                    }

                    Object constraintValues = st.algorithm.get("constraints");

                    final Set<InitializationMsgJson.Constraint> constraints = new HashSet<>();
                    if (constraintValues instanceof Map) {
                        for (Object entry : ((Map<String, Object>) constraintValues).values()) {
                            if (!(entry instanceof Map)) {
                                // TODO: error constraint is dont a map, cannot parse constraint
                            }
                        }

                        Map<String, Map<String, Object>> namedConstraints = (Map<String, Map<String, Object>>) constraintValues;

                        for (Map.Entry<String, Map<String, Object>> entry : namedConstraints.entrySet()) {
                            final InitializationMsgJson.Constraint constraint = InitializationMsgJson.Constraint.parse(entry.getValue());
                            constraint.setId(entry.getKey());
                            constraints.add(constraint);
                        }
                    } else {
                        // TODO: error constraints does not contain map of named constraints
                    }

                    stepSizeCalculator = new VariableStepSizeCalculator(constraints, stepsizeInterval, initsize);
                    algorithm = Algorithm.VARSTEP;

                    logger.info("Using Variable-step size calculator.");
                }
            }

            if (stepSizeCalculator == null) {
                algorithm = Algorithm.FIXED;
                stepSizeCalculator = new BasicFixedStepSizeCalculator(0.1);
                logger.info("No step size algorithm given. Defaulting to fixed-step with size 0.1");
            }

            Map<String, List<ModelDescription.LogCategory>> logs = null;
            Coe coe = sessionController.getCoe(sessionId);
            try {
                coe.getConfiguration().isStabalizationEnabled = st.stabalizationEnabled;
                coe.getConfiguration().global_absolute_tolerance = st.global_absolute_tolerance;
                coe.getConfiguration().global_relative_tolerance = st.global_relative_tolerance;
                coe.getConfiguration().loggingOn = st.loggingOn;
                coe.getConfiguration().visible = st.visible;
                coe.getConfiguration().parallelSimulation = st.parallelSimulation;
                coe.getConfiguration().simulationProgramDelay = st.simulationProgramDelay;
                coe.getConfiguration().hasExternalSignals = st.hasExternalSignals;
                logs = coe.initialize(st.getFmuFiles(), buildConnections(st.connections), buildParameters(st.parameters), stepSizeCalculator,
                        new LogVariablesContainer(buildVariableMap(st.livestream), buildVariableMap(st.logVariables)));
                if (algorithm == Algorithm.VARSTEP && !coe.canDoVariableStep()) {
                    logger.error("Initialization failed: One or more FMUs cannot perform variable step size");
                    return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                            "One or more FMUs does not support variable step size.");
                }
                logger.trace("Initialization completed obtained the following logging categories: {}", logs);
            } catch (Exception e) {
                logger.error("Internal error in initialization", e);
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
            }

            FileUtils.write(new File(coe.getResultRoot(), "initialize.json"), data);
            InitializationStatusJson ret = new InitializationStatusJson(sessionController.getStatus(sessionId), logs);
            return NanoWSDImpl.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON,
                    mapper.writeValueAsString(new ArrayList<>(Arrays.asList(ret))));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Initialization failed", e);

            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
        }
    }

    private NanoHTTPD.Response simulate(String sessionId, String data, boolean async) throws IOException {
        if (data.isEmpty()) {
            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "missing json data for configuration");
        }

        StartMsgJson st = mapper.readValue(data, StartMsgJson.class);

        Coe coe = sessionController.getCoe(sessionId);
        //The Coe must not be uninitialized
        if (coe.getState().equals(CoeStatus.Unitialized.toString())) {
            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    "The Co-Simulation Orchestration Engine has not been initialized and can therefore not begin simulation.");
        }

        FileUtils.write(new File(coe.getResultRoot(), "simulate.json"), data);

        Map<ModelConnection.ModelInstance, List<String>> logLevels = new HashMap<>();

        if (st.logLevels != null) {
            for (Map.Entry<String, List<String>> entry : st.logLevels.entrySet()) {
                try {
                    logLevels.put(ModelConnection.ModelInstance.parse(entry.getKey()), entry.getValue());
                } catch (Exception e) {
                    return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Error in logging levels");
                }
            }
        }

        if (!async) {
            try {
                coe.simulate(st.startTime, st.endTime, logLevels, st.reportProgress, st.liveLogInterval);
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON,
                        mapper.writeValueAsString(sessionController.getStatus(sessionId)));
            } catch (Exception e) {
                logger.error("Error in simulation", e);
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            (new Thread(() -> {
                try {
                    coe.simulate(st.startTime, st.endTime, logLevels, st.reportProgress, st.liveLogInterval);
                } catch (Exception e) {
                    coe.setLastError(e);
                }
            })).start();
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON,
                    mapper.writeValueAsString(sessionController.getStatus(sessionId)));
        }
    }

    //Todo: Add check to see if coe is initialized
    public NanoHTTPD.Response processSimulate(String sessionId, String data, boolean async) throws IOException {
        return simulate(sessionId, data, async);
    }

    public NanoHTTPD.Response processResult(String sessionId, boolean zip) throws IOException {
        Coe coe = sessionController.getCoe(sessionId);

        if (zip) {
            File resultFolder = coe.getResultRoot();
            if (resultFolder == null) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "No result folder exists yet");
            }

            File temp = File.createTempFile("coe-zip", ".zip");
            temp = zipDirectoryLarge(resultFolder, temp);

            if (!temp.exists() || !temp.canRead()) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "Unable to read generated zip achieve");
            }

            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, Response.MIME_ZIP, new DeleteOnCloseFileInputStream(temp));
        } else {
            File resultFile = coe.getResult();
            if (resultFile == null || !resultFile.isFile()) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "No result file exists yet");
            }

            if (!resultFile.exists() || !resultFile.canRead()) {
                return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "Unable to access result file");
            }

            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, Response.MIME_PLAINTEXT, new FileInputStream(resultFile));
        }

    }

    public NanoHTTPD.Response processDestroy(String sessionId) throws IOException {
        Util.removeCoSimInstanceLogAppenders(sessionId);
        File resultFile = this.sessionController.getSessionRootDir(sessionId);
        logger.debug("Deleting directory {}.", resultFile.getPath());
        FileUtils.deleteDirectory(resultFile);
        this.sessionController.removeSession(sessionId);
        logger.debug("Session {} destroyed.", sessionId);
        return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, "Session " + sessionId + " destroyed");
    }

    public NanoHTTPD.Response processVersion() {
        Properties prop = new Properties();
        try {
            InputStream coeProp = getClass().getResourceAsStream("/coe.properties");
            prop.load(coeProp);

            final String message = "{\"version\":\"%s\",\n\"artifactId\":\"%s\",\n\"groupId\":\"%s\"\n}";
            String m = String.format(message, prop.getProperty("version"), prop.getProperty("artifactId"), prop.getProperty("groupId"));
            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, m);
        } catch (IOException e) {
            return ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, e.getMessage());
        }

    }

}
