package org.intocps.maestro.interpreter.values.variablestep;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.ValueExtractionUtilities;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.io.File;
import java.net.URI;
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
     * @param configUri
     * @return
     */
    private static Map<String, Value> createMembers(String configUri) {
        Set<InitializationMsgJson.Constraint> constraints;
        StepsizeInterval stepsizeInterval;
        Double initSize;

        try {
            URI pathAsUri = URI.create((new URI(configUri)).getRawPath());
            if (!pathAsUri.isAbsolute()) {
                pathAsUri = (new File(".")).toURI().resolve(pathAsUri);
            }
            InitializationMsgJson config = (new ObjectMapper()).readValue(new String(Files.readAllBytes(Paths.get(pathAsUri))), InitializationMsgJson.class);
            constraints = getConstraintsFromConfig(config);
            stepsizeInterval = getStepSizeIntervalFromConfig(config);
            initSize = getInitSizeFromConfig(config);
        } catch (Exception e) {
            throw new InterpreterException("Configuration could not be passed", e);
        }

        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("setFMUs", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);
            try {
                List<StringValue> fmuInstanceNames = ValueExtractionUtilities.getArrayValue(fcargs.get(0), StringValue.class);
                List<FmuValue> fmus = ValueExtractionUtilities.getArrayValue(fcargs.get(1), FmuValue.class);

                Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances = new HashMap<>();

                for (int i = 0; i < fmuInstanceNames.size(); i++) {
                    String instanceName = fmuInstanceNames.get(i).getValue().split("\\.")[1]; //TODO: This should be more dynamic
                    String fmuName = fmuInstanceNames.get(i).getValue().split("\\.")[0];
                    ModelConnection.ModelInstance mi = new ModelConnection.ModelInstance(fmuName, instanceName);
                    IFmu fmu = fmus.get(i).getModule();
                    ModelDescription modelDescription = new ModelDescription(fmu.getModelDescription());
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

            Value id = fcargs.get(0).deref();
            VariableStepConfigValue variableStepConfig = (VariableStepConfigValue) id;
            variableStepConfig.initializePorts(
                    ValueExtractionUtilities.getArrayValue(fcargs.get(1), StringValue.class).stream().map(StringValue::getValue)
                            .collect(Collectors.toList()));
            return new VoidValue();
        }));
        componentMembers.put("addDataPoint", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 3);

            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue) id;
                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> arrayValue = ValueExtractionUtilities.getArrayValue(fcargs.get(2), Value.class);
                List<VariableStepConfigValue.StepVal> typesToValues = new ArrayList<>();

                for (int i = 0; i < arrayValue.size(); i++) {
                    Object value = arrayValue.get(i);

                    if (value instanceof StringValue) {
                        typesToValues.add(new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((StringValue) value).getValue()));
                    } else if (value instanceof IntegerValue) {
                        typesToValues.add(new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((IntegerValue) value).getValue()));
                    } else if (value instanceof RealValue) {
                        typesToValues
                                .add(new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i), ((RealValue) value).getValue()));
                    } else if (value instanceof BooleanValue) {
                        typesToValues.add(new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((BooleanValue) value).getValue()));
                    } else if (value instanceof EnumerationValue) {
                        typesToValues.add(new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((EnumerationValue) value).getValue()));
                    }
                }
                variableStepConfigValue.addDataPoint(time, typesToValues);
            }
            return new VoidValue();
        }));
        componentMembers.put("getStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 1);

            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {
                return new RealValue(((VariableStepConfigValue) id).getStepSize());
            }
            return new RealValue(-1.0);
        }));
        componentMembers.put("setEndTime", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgLength(fcargs, 2);
            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {
                ((VariableStepConfigValue) id).setEndTime(((RealValue) fcargs.get(1).deref()).getValue());
            }
            return new VoidValue();
        }));

        return componentMembers;
    }

}
