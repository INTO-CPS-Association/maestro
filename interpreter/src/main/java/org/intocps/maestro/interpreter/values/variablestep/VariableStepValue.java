package org.intocps.maestro.interpreter.values.variablestep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.FmiInvalidNativeStateException;
import org.intocps.fmi.FmuResult;
import org.intocps.fmi.IFmiComponent;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.fmi.FmiInstanceConfig;
import org.intocps.maestro.fmi.FmiSimulationInstance;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.fmi.ModelDescription;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class VariableStepValue extends ModuleValue {

    public VariableStepValue(String configuration) {
        super(createMembers(configuration));
    }


    private static Set<InitializationMsgJson.Constraint> getConstraintsFromConfig(JsonNode config) {
        final Set<InitializationMsgJson.Constraint> constraints = new HashSet<>();
        Map<String, Map<String, Object>> namedConstraints = (new ObjectMapper()).convertValue(config.get("constraints"), new TypeReference<>() {
        });
        if(namedConstraints == null){
            return constraints;
        }

        for (Map.Entry<String, Map<String, Object>> entry : namedConstraints.entrySet()) {
            final InitializationMsgJson.Constraint constraint = InitializationMsgJson.Constraint.parse(entry.getValue());
            constraint.setId(entry.getKey());
            constraints.add(constraint);
        }
        return constraints;
    }

    private static StepsizeInterval getStepSizeIntervalFromConfig(JsonNode config) {
        List<Double> sizes = new Vector<>();
        List<Object> objSizes = (new ObjectMapper()).convertValue(config.get("size"), new TypeReference<>() {
        });

        for (Object number : objSizes) {
            if (number instanceof Integer) {
                sizes.add(Double.valueOf((Integer) number));
            } else if (number instanceof Double) {
                sizes.add((Double) number);
            }
        }
        return new StepsizeInterval(sizes.get(0), sizes.get(1));
    }

    private static Double getInitSizeFromConfig(JsonNode config) {
        Double initsize = -1.0;
        Object objInitsize = (new ObjectMapper()).convertValue(config.get("initsize"), new TypeReference<>() {
        });

        if (objInitsize instanceof Double) {
            initsize = (Double) objInitsize;
        } else if (objInitsize instanceof Integer) {
            initsize = Double.valueOf((Integer) objInitsize);
        }
        return initsize;
    }

    private static Double calculateMaxStepSize(Double minStepSize, Double maxStepSize, Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances){
        boolean allGreaterOrEqualMaxSize = true;
        Double stepSize = Double.MAX_VALUE;
        for(FmiSimulationInstance instance : instances.values()){
            try {
                if(instance.instance != null){
                    FmuResult<Double> res = instance.instance.getMaxStepSize();
                    if(res.status != Fmi2Status.Error){
                        double maxSize = res.result;

                        //If ANY are less than minStepsize, choose minStepsize.
                        if(maxSize < minStepSize){
                            return minStepSize;
                        }
                        allGreaterOrEqualMaxSize = allGreaterOrEqualMaxSize && maxSize >= maxStepSize;

                        stepSize = Math.min(maxSize, stepSize);
                    }

                }
            } catch (FmiInvalidNativeStateException e) {
                //TODO: log event
            }
        }

        //If ALL are greater than maxStepsize, choose maxStepsize
        //If NONE succeeds in getMaxStepSize, choose maxStepsize
        if(allGreaterOrEqualMaxSize || stepSize.equals(Double.MAX_VALUE)){
            return maxStepSize;
        }

        //If ANY are between minStepsize and maxStepsize AND none are less than minStepsize, then choose minimum of these.
        return stepSize;
    }

    /**
     * Values passed in addDataPoint is assumed to follow the same order as the portNames passed in initializePortNames.
     *
     * @param algorithmAsUriOrJson
     * @return
     */
    private static Map<String, Value> createMembers(String algorithmAsUriOrJson) {
        Set<InitializationMsgJson.Constraint> constraints;
        StepsizeInterval stepsizeInterval;
        Double initSize;

        // Try to generate config from uri
        JsonNode config = null;
        try {
            URI pathAsUri = URI.create((new URI(algorithmAsUriOrJson)).getRawPath());
            if (!pathAsUri.isAbsolute()) {
                pathAsUri = (new File(".")).toURI().resolve(pathAsUri);
            }
            config = (new ObjectMapper()).readTree(new String(Files.readAllBytes(Paths.get(pathAsUri))));
        } catch (URISyntaxException | IOException e) {
            if (e instanceof IOException) {
                throw new InterpreterException("Configuration could not be passed", e);
            }
        }

        // Try to generate config from json
        if (config == null) {
            try {
                config = (new ObjectMapper()).readTree(StringEscapeUtils.unescapeJava(algorithmAsUriOrJson));
            } catch (JsonProcessingException e) {
                throw new InterpreterException("Configuration could not be passed", e);
            }
        }

        if (config != null) {
            constraints = getConstraintsFromConfig(config);
            stepsizeInterval = getStepSizeIntervalFromConfig(config);
            initSize = getInitSizeFromConfig(config);
        } else {
            throw new InterpreterException("Configuration could not be passed");
        }

        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("setFMUs", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);
            try {
                List<StringValue> fullyQualifiedFMUInstanceNames = ValueExtractionUtilities.getArrayValue(fcargs.get(0), StringValue.class);
                List<FmuComponentValue> fmus = ValueExtractionUtilities.getArrayValue(fcargs.get(1), FmuComponentValue.class);

                Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances = new HashMap<>();

                for (int i = 0; i < fullyQualifiedFMUInstanceNames.size(); i++) {
                    String instanceName =
                            fullyQualifiedFMUInstanceNames.get(i).getValue().split("\\.")[1]; //TODO: This should probably be done in another way
                    String fmuName = fullyQualifiedFMUInstanceNames.get(i).getValue().split("\\.")[0];
                    ModelConnection.ModelInstance mi = new ModelConnection.ModelInstance(fmuName, instanceName);
                    IFmiComponent fmu = fmus.get(i).getModule();
                    ModelDescription modelDescription = new ModelDescription(fmu.getFmu().getModelDescription());
                    FmiInstanceConfig fmiInstanceConfig = new FmiInstanceConfig(modelDescription, modelDescription.getScalarVariables());
                    FmiSimulationInstance si = new FmiSimulationInstance(null, fmiInstanceConfig);
                    instances.put(mi, si);
                }
                return new VariableStepConfigValue(instances, constraints, stepsizeInterval, initSize,
                        calculateMaxStepSize(stepsizeInterval.getMinimalStepsize(), stepsizeInterval.getMaximalStepsize(), instances));
            } catch (Exception e) {
                throw new InterpreterException("Could not set FMUs", e);
            }
        }));
        componentMembers.put("initializePortNames", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfig = (VariableStepConfigValue) cv;
                variableStepConfig.initializePorts(
                        ValueExtractionUtilities.getArrayValue(fcargs.get(1), StringValue.class).stream().map(StringValue::getValue).collect(Collectors.toList()));
            }
            else {
                throw new InterpreterException("Invalid arguments");
            }
            return new VoidValue();
        }));
        componentMembers.put("addDataPoint", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 3);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> portValues = ValueExtractionUtilities.getArrayValue(fcargs.get(2), Value.class);
                variableStepConfigValue.addDataPoint(time, portValues);
            }
            else{
                throw new InterpreterException("Invalid arguments");
            }
            return new VoidValue();
        }));
        componentMembers.put("getStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                return new RealValue(variableStepConfigValue.getStepSize());
            }
            throw new InterpreterException("Invalid arguments");
        }));
        componentMembers.put("setEndTime", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);
            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                ((VariableStepConfigValue) cv).setEndTime(((RealValue) fcargs.get(1).deref()).getValue());
            }
            else{
                throw new InterpreterException("Invalid arguments");
            }
            return new VoidValue();
        }));
        componentMembers.put("isStepValid", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 4);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                double nextTime = ((RealValue) fcargs.get(1).deref()).getValue();
                boolean supportsRollBack = ((BooleanValue) fcargs.get(2).deref()).getValue();
                List<Value> portValues = ValueExtractionUtilities.getArrayValue(fcargs.get(3), Value.class);
                return new BooleanValue(variableStepConfigValue.isStepValid(nextTime, portValues, supportsRollBack));
            } else {
                throw new InterpreterException("Invalid arguments");
            }
        }));
        componentMembers.put("hasReducedStepsize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                return new BooleanValue(variableStepConfigValue.hasReducedStepSize());
            } else {
                throw new InterpreterException("Invalid arguments");
            }
        }));
        componentMembers.put("getReducedStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                return new RealValue(variableStepConfigValue.getReducedStepSize());
            } else {
                throw new InterpreterException("Invalid arguments");
            }
        }));

        return componentMembers;
    }

}
