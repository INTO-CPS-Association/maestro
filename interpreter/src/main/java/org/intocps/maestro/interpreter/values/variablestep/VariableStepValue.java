package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.interpreter.values.utilities.ArrayUtilValue;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class VariableStepValue extends ModuleValue {

    public VariableStepValue(String configuration) {
        super(createMembers());
        //        ObjectMapper om = new ObjectMapper();
        //        ConstraintsJson constraintsJson = om.readValue(configuration, ConstraintsJson.class);
        //        constraintsJson.constraints

    }


    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("setFMUs", new FunctionValue.ExternalFunctionValue(fcargs -> {
            try {
                if (fcargs == null) {
                    throw new InterpreterException("No values passed");
                }

                if (fcargs.stream().anyMatch(Objects::isNull)) {
                    throw new InterpreterException("Argument list contains null values");
                }

                List<StringValue> fmuNames = ArrayUtilValue.getArrayValue(fcargs.get(1), StringValue.class);
                List<FmuValue> fmus = ArrayUtilValue.getArrayValue(fcargs.get(1), FmuValue.class);

                Map<ModelConnection.ModelInstance, FmiSimulationInstance> instances = new HashMap<>();

                for (int i = 0; i < fmuNames.size(); i++) {
                    ModelConnection.ModelInstance mi = new ModelConnection.ModelInstance(fmuNames.get(i).getValue(), null);
                    IFmu fmu = fmus.get(i).getModule();
                    ModelDescription modelDescription = new ModelDescription(fmu.getModelDescription());
                    FmiInstanceConfig fmiInstanceConfig = new FmiInstanceConfig(modelDescription, modelDescription.getScalarVariables());
                    FmiSimulationInstance si = new FmiSimulationInstance(null, fmiInstanceConfig);
                    instances.put(mi, si);
                }
                return new VariableStepConfigValue(instances);
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

                double time = ((RealValue) fcargs.get(1).deref()).getValue();
                List<Value> arrayValue = fcargs.stream().skip(2).map(Value::deref).collect(Collectors.toList());
                Map<ModelDescription.Types, Object> typesToValues = new HashMap<>();
                arrayValue.forEach(x -> {
                    // TODO: These needs to be affiliated with portnames. Perhaps in the VariableStepConfigValue.VarStepValue?
                    if (x instanceof StringValue) {
                        typesToValues.put(ModelDescription.Types.String, ((StringValue) x).getValue());
                    }
                    // TODO: Int, real, boolean, enumeration
                });
                ((VariableStepConfigValue) id).addDataPoint(time, typesToValues);
            }

            return new VoidValue();
        }));
        componentMembers.put("getStepSize", new FunctionValue.ExternalFunctionValue(fcargs -> {
            Value id = fcargs.get(0).deref();
            if (id instanceof VariableStepConfigValue) {

                VariableStepConfigValue id_ = (VariableStepConfigValue) id;
                return new RealValue(id_.getStepSize());
            }
            return new RealValue(-1.0);
        }));

        return componentMembers;
    }

}
