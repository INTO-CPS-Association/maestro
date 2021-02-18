package org.intocps.maestro.interpreter.values.variablestep;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.interpreter.values.utilities.ArrayUtilValue;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.*;
import java.util.stream.Collectors;

public class VariableStepValue extends ModuleValue {

    public VariableStepValue(String configuration){
        super(createMembers(configuration));
    }

    private static Set<InitializationMsgJson.Constraint> getConstraintsFromConfig(InitializationMsgJson config){
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

    private static StepsizeInterval getStepsizeIntervalFromConfig(InitializationMsgJson config) {
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

    private static Double getInitSizeFromConfig(InitializationMsgJson config){
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
     * @param configJson
     * @return
     */
    private static Map<String, Value> createMembers(String configJson) {
        Set<InitializationMsgJson.Constraint> constraints;
        StepsizeInterval stepsizeInterval;
        Double initSize;

        try{
            InitializationMsgJson config = (new ObjectMapper()).readValue(configJson, InitializationMsgJson.class);
            constraints = getConstraintsFromConfig(config);
            stepsizeInterval = getStepsizeIntervalFromConfig(config);
            initSize = getInitSizeFromConfig(config);
        }
        catch (Exception e){
            throw new InterpreterException("Configuration could not be passed", e);
        }

        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("setFMUs", new FunctionValue.ExternalFunctionValue(fcargs -> {
            try {
                if (fcargs == null) {
                    throw new InterpreterException("No values passed");
                }

                if (fcargs.stream().anyMatch(Objects::isNull)) {
                    throw new InterpreterException("Argument list contains null values");
                }

                List<StringValue> fmuInstanceNames = ArrayUtilValue.getArrayValue(fcargs.get(1), StringValue.class);
                List<FmuValue> fmus = ArrayUtilValue.getArrayValue(fcargs.get(1), FmuValue.class);

                Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances = new HashMap<>();

                for (int i = 0; i < fmuInstanceNames.size(); i++) {
                    ModelConnection.ModelInstance mi = new ModelConnection.ModelInstance(fmuInstanceNames.get(i).getValue(), null);
                    IFmu fmu = fmus.get(i).getModule();
                    ModelDescription modelDescription = new ModelDescription(fmu.getModelDescription());
                    FmiInstanceConfig fmiInstanceConfig = new FmiInstanceConfig(modelDescription, modelDescription.getScalarVariables());
                    FmiSimulationInstance si = new FmiSimulationInstance(null, fmiInstanceConfig);
                    instances.put(mi, si);
                }
                return new VariableStepConfigValue(instances, constraints, stepsizeInterval, initSize);
            } catch (Exception e) {
                return null;
            }
        }));
        // {{fmuname}.instancename.portName, {fmuname}.instancename.portName}
        componentMembers.put("initializePortNames", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            Value id = fcargs.get(0).deref();
            VariableStepConfigValue variableStepConfig = (VariableStepConfigValue) id;
            variableStepConfig.initializePorts(
                    ArrayUtilValue.getArrayValue(fcargs.get(0), StringValue.class).stream().map(x -> x.getValue()).collect(Collectors.toList()));
            return new VoidValue();
        }));
        componentMembers.put("addDataPoint", new FunctionValue.ExternalFunctionValue(fcargs -> {
            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {
                VariableStepConfigValue variableStepConfigValue = (VariableStepConfigValue)id;
                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> arrayValue = fcargs.stream().skip(2).map(Value::deref).collect(Collectors.toList());
                Map<ModelDescription.Types, VariableStepConfigValue.StepVal> typesToValues = new HashMap<>();

                //TODO: Should this type conversion and linking of port names and values be done in VariableStepConfig instead?
                for(int i = 0; i < arrayValue.size(); i++) {
                    Object value = arrayValue.get(i);

                    if (value instanceof StringValue) {
                        typesToValues.put(ModelDescription.Types.String, new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i), ((StringValue) value).getValue()));
                    }
                    else if (value instanceof IntegerValue) {
                        typesToValues.put(ModelDescription.Types.Integer, new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((IntegerValue) value).getValue()));
                    }
                    else if (value instanceof RealValue) {
                        typesToValues.put(ModelDescription.Types.Real, new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((RealValue) value).getValue()));
                    }
                    else if (value instanceof BooleanValue) {
                        typesToValues.put(ModelDescription.Types.Boolean, new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((BooleanValue) value).getValue()));
                    }
                    else if (value instanceof EnumerationValue) {
                        typesToValues.put(ModelDescription.Types.Enumeration, new VariableStepConfigValue.StepVal(variableStepConfigValue.getPorts().get(i),
                                ((EnumerationValue) value).getValue()));
                    }

                }
                variableStepConfigValue.addDataPoint(time, typesToValues);
            }
            return new VoidValue();
        }));
        componentMembers.put("getStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {
                return new RealValue(((VariableStepConfigValue) id).getStepSize());
            }
            return new RealValue(-1.0);
        }));

        return componentMembers;
    }

}
