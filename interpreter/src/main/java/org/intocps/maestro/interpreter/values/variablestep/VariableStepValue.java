package org.intocps.maestro.interpreter.values.variablestep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.fmi.IFmiComponent;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

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


    private static Set<InitializationMsgJson.Constraint> getConstraintsFromConfig(InitializationMsgJson config) {
        final Set<InitializationMsgJson.Constraint> constraints = new HashSet<>();
        Object constraintValues = config.algorithm.get("constraints");
        Map<String, Map<String, Object>> namedConstraints = (Map<String, Map<String, Object>>) constraintValues;

        for (Map.Entry<String, Map<String, Object>> entry : namedConstraints.entrySet()) {
            final InitializationMsgJson.Constraint constraint = InitializationMsgJson.Constraint.parse(entry.getValue());
            constraint.setId(entry.getKey());
            constraints.add(constraint);
        }
        return constraints;
    }

    private static StepsizeInterval getStepSizeIntervalFromConfig(InitializationMsgJson config) {
        List<Double> size = new Vector<>();
        Object objSize = config.algorithm.get("size");
        for (Object number : (List<Object>) objSize) {
            if (number instanceof Integer) {
                number = new Double((Integer) number);
            }

            if (number instanceof Double) {
                size.add((Double) number);
            }
        }
        return new StepsizeInterval(size.get(0), size.get(1));
    }

    private static Double getInitSizeFromConfig(InitializationMsgJson config) {
        Double initsize = -1.0;
        Object objInitsize = config.algorithm.get("initsize");
        if (objInitsize instanceof Double) {
            initsize = (Double) objInitsize;
        } else if (objInitsize instanceof Integer) {
            initsize = new Double((Integer) objInitsize);
        }
        return initsize;
    }

    /**
     * Values passed in addDataPoint is assumed to follow the same order as the portNames passed in initializePortNames.
     *
     * @param configAsUriOrJson
     * @return
     */
    private static Map<String, Value> createMembers(String configAsUriOrJson) {
        Set<InitializationMsgJson.Constraint> constraints;
        StepsizeInterval stepsizeInterval;
        Double initSize;

        // Try to generate config from uri
        InitializationMsgJson config = null;
        try {
            URI pathAsUri = URI.create((new URI(configAsUriOrJson)).getRawPath());
            if (!pathAsUri.isAbsolute()) {
                pathAsUri = (new File(".")).toURI().resolve(pathAsUri);
            }
            config = (new ObjectMapper()).readValue(new String(Files.readAllBytes(Paths.get(pathAsUri))), InitializationMsgJson.class);
        } catch (URISyntaxException | IOException e) {
            if(e instanceof IOException){
                throw new InterpreterException("Configuration could not be passed", e);
            }
        }

        // Try to generate config from json
        if(config == null){
            try {
                config = (new ObjectMapper()).readValue(StringEscapeUtils.unescapeJava(configAsUriOrJson), InitializationMsgJson.class);
            } catch (JsonProcessingException e) {
                throw new InterpreterException("Configuration could not be passed", e);
            }
        }

        if(config != null){
            constraints = getConstraintsFromConfig(config);
            stepsizeInterval = getStepSizeIntervalFromConfig(config);
            initSize = getInitSizeFromConfig(config);
        }
        else{
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
                    String instanceName = fullyQualifiedFMUInstanceNames.get(i).getValue().split("\\.")[1]; //TODO: This should probably be done in another way
                    String fmuName = fullyQualifiedFMUInstanceNames.get(i).getValue().split("\\.")[0];
                    ModelConnection.ModelInstance mi = new ModelConnection.ModelInstance(fmuName, instanceName);
                    IFmiComponent fmu = fmus.get(i).getModule();
                    ModelDescription modelDescription = new ModelDescription(fmu.getFmu().getModelDescription());
                    FmiInstanceConfig fmiInstanceConfig = new FmiInstanceConfig(modelDescription, modelDescription.getScalarVariables());
                    FmiSimulationInstance si = new FmiSimulationInstance(null, fmiInstanceConfig);
                    instances.put(mi, si);
                }
                return new VariableStepConfigValue(instances, constraints, stepsizeInterval, initSize);
            } catch (Exception e) {
                throw new InterpreterException("Could not set FMUs", e);
            }
        }));
        componentMembers.put("initializePortNames", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);

            Value cv = fcargs.get(0).deref();
            VariableStepConfigValue variableStepConfig = (VariableStepConfigValue) cv;
            variableStepConfig.initializePorts(
                    ValueExtractionUtilities.getArrayValue(fcargs.get(1), StringValue.class).stream().map(StringValue::getValue)
                            .collect(Collectors.toList()));
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
            return new VoidValue();
        }));
        componentMembers.put("getStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                return new RealValue(variableStepConfigValue.getStepSize());
            }
            return new RealValue(-1.0);
        }));
        componentMembers.put("setEndTime", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);
            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                ((VariableStepConfigValue) cv).setEndTime(((RealValue) fcargs.get(1).deref()).getValue());
            }
            return new VoidValue();
        }));
        componentMembers.put("isStepValid", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 3);

            Value cv = fcargs.get(0).deref();
            if (cv instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) cv;
                double nextTime = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> portValues = ValueExtractionUtilities.getArrayValue(fcargs.get(2), Value.class);
                return new BooleanValue(variableStepConfigValue.isStepValid(nextTime, portValues));
            } else {
                return new BooleanValue(false);
            }
        }));

        return componentMembers;
    }

}
