package org.intocps.orchestration.coe.webapi.services;

import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvironmentFMU {
    final String key;
    private List<ModelDescription.ScalarVariable> inputs;
    private Set<ModelDescription.ScalarVariable> outputs;
    final String fmuName;
    final String instanceName;

    public EnvironmentFMU(String fmuName, String instanceName) {
        this.key = fmuName;
        this.fmuName = fmuName;
        this.instanceName = instanceName;
    }

    private Object start(ModelDescription.Type type) {
        if (type.start != null) {
            return type.start;
        } else {
            switch (type.type) {
                case Boolean:
                    return false;
                case Real:
                    return 0.0;
                case Integer:
                case Enumeration:
                    return 0;
                case String:
                    return "";
                default:
                    return null;
            }
        }
    }

    /**
     * Calculates the inputs to the environment based on the outputs of the Single FMU.
     * Invariant:
     * The name and value reference of a calculated input variable shall be identical to its related output variable.
     * @param outputs
     */
    public void calculateInputs(Set<ModelDescription.ScalarVariable> outputs) {
        this.inputs = outputs.stream().map(sv -> {
            ModelDescription.ScalarVariable inpSv = new ModelDescription.ScalarVariable();
            inpSv.causality = ModelDescription.Causality.Input;
            inpSv.type = new ModelDescription.Type();
            inpSv.type.type = sv.type.type;
            inpSv.variability = ModelDescription.Variability.Continuous;
            inpSv.type.start = start(sv.type);
            inpSv.valueReference = sv.valueReference;
            inpSv.name = sv.name;
            return inpSv;
        }).collect(Collectors.toList());
    }

    /**
     * Calculates the outputs from the environment based on the inputs of the Single FMU
     * Invariant:
     * The name and value reference of a calculated out variable shall be identical to its related input variable.
     */
    public void calculateOutputs(ModelDescription singleFMU) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        Stream<ModelDescription.ScalarVariable> singleFMUInputs = singleFMU.getScalarVariables().stream().filter(scalarVariable -> scalarVariable.causality == ModelDescription.Causality.Input);
        this.outputs = singleFMUInputs.map(scalarVariable -> {
            ModelDescription.ScalarVariable outputScalarVariable = new ModelDescription.ScalarVariable();
            outputScalarVariable.causality = ModelDescription.Causality.Output;
            outputScalarVariable.variability = ModelDescription.Variability.Continuous;
            outputScalarVariable.type = new ModelDescription.Type();
            outputScalarVariable.type.type = scalarVariable.type.type;
            outputScalarVariable.valueReference = scalarVariable.valueReference;
            outputScalarVariable.name = scalarVariable.name;
            return outputScalarVariable;
        }).collect(Collectors.toSet());
    }

    public List<ModelDescription.ScalarVariable> getInputs() {
        return inputs;
    }

    public Set<ModelDescription.ScalarVariable> getOutputs() {
        return outputs;
    }
}
