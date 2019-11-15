package org.intocps.orchestration.coe.webapi.services;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;

public class EnvironmentFMU implements IFmu {
    private static EnvironmentFMU environmentFMU;
    final String key;
    final String fmuName;
    final String instanceName;
    // A map from the non-virtual variable to the corresponding virtual scalar variable in the environment FMU.
    private final Map<ModelConnection.Variable, ModelDescription.ScalarVariable> sourceToEnvironmentVariable = new HashMap<>();
    // The inputs of the environment FMU
    private final List<ModelDescription.ScalarVariable> inputs = new ArrayList<>();
    // The outputs of the environment FMU
    private final List<ModelDescription.ScalarVariable> outputs = new ArrayList<>();
    private Integer nextValueReference = 1;
    private String modelDescriptionXML;
    private EnvironmentFMUComponent environmentFMUComponent;

    public EnvironmentFMU(String fmuName, String instanceName) {
        this.key = "{" + fmuName + "}";
        this.fmuName = fmuName;
        this.instanceName = instanceName;
    }

    public static EnvironmentFMU getInstance() {

        return environmentFMU;
    }

    public static EnvironmentFMU CreateEnvironmentFMU(String fmuName, String instanceName) {
        environmentFMU = new EnvironmentFMU(fmuName, instanceName);
        return getInstance();
    }

    public Map<ModelConnection.Variable, ModelDescription.ScalarVariable> getSourceToEnvironmentVariable() {
        return sourceToEnvironmentVariable;
    }

    public ModelConnection.Variable createVariable(ModelDescription.ScalarVariable sv) throws InvalidVariableStringException {
        return ModelConnection.Variable.parse(this.key + "." + this.instanceName + "." + sv.name);
    }

    public void createModelDescriptionXML() {
        modelDescriptionXML = EnvironmentFMUModelDescription.CreateEnvironmentFMUModelDescription(inputs, outputs, fmuName);
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
     *
     * @param outputs
     */
//    public void calculateInputs(List<ModelDescription.ScalarVariable> outputs) {
//        this.inputs = outputs.stream().map(sv -> {
//            ModelDescription.ScalarVariable inpSv = new ModelDescription.ScalarVariable();
//            inpSv.causality = ModelDescription.Causality.Input;
//            inpSv.type = new ModelDescription.Type();
//            inpSv.type.type = sv.type.type;
//            inpSv.variability = ModelDescription.Variability.Discrete;
//            inpSv.type.start = start(sv.type);
//            inpSv.valueReference = sv.valueReference;
//            inpSv.name = sv.name;
//            return inpSv;
//        }).collect(Collectors.toList());
//    }

    /**
     * Calculates the outputs from the environment based on the inputs of the Single FMU
     * Invariant:
     * The name and value reference of a calculated out variable shall be identical to its related input variable.
     */
//    public void calculateOutputs(ModelDescription singleFMU) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
//        Stream<ModelDescription.ScalarVariable> singleFMUInputs = singleFMU.getScalarVariables().stream().filter(scalarVariable -> scalarVariable.causality == ModelDescription.Causality.Input);
//        this.outputs = singleFMUInputs.map(scalarVariable -> {
//            ModelDescription.ScalarVariable outputScalarVariable = new ModelDescription.ScalarVariable();
//            outputScalarVariable.causality = ModelDescription.Causality.Output;
//            outputScalarVariable.variability = ModelDescription.Variability.Discrete;
//            outputScalarVariable.type = new ModelDescription.Type();
//            outputScalarVariable.type.type = scalarVariable.type.type;
//            outputScalarVariable.valueReference = scalarVariable.valueReference;
//            outputScalarVariable.name = scalarVariable.name;
//            return outputScalarVariable;
//        }).collect(Collectors.toList());
//    }
    private ModelDescription.ScalarVariable createOutputScalarVariable(ModelDescription.ScalarVariable sourceScalarVariable, long valueReference) {
        ModelDescription.ScalarVariable outputScalarVariable = new ModelDescription.ScalarVariable();
        outputScalarVariable.causality = ModelDescription.Causality.Output;
        outputScalarVariable.variability = ModelDescription.Variability.Discrete;
        outputScalarVariable.type = new ModelDescription.Type();
        outputScalarVariable.type.type = sourceScalarVariable.type.type;
        outputScalarVariable.valueReference = valueReference;
        outputScalarVariable.initial = sourceScalarVariable.initial;
        // valueReference added to name to ensure uniqueness
        outputScalarVariable.name = sourceScalarVariable.name + valueReference;
        return outputScalarVariable;
    }

    private ModelDescription.ScalarVariable createInputScalarVariable(ModelDescription.ScalarVariable sourceScalarVariable, long valueReference) {
        ModelDescription.ScalarVariable inpSv = new ModelDescription.ScalarVariable();
        inpSv.causality = ModelDescription.Causality.Input;
        inpSv.type = new ModelDescription.Type();
        inpSv.type.type = sourceScalarVariable.type.type;
        inpSv.variability = ModelDescription.Variability.Discrete;
        inpSv.type.start = start(sourceScalarVariable.type);
        inpSv.valueReference = valueReference;
        // valueReference added to name to ensure uniqueness
        inpSv.name = sourceScalarVariable.name + valueReference;
        return inpSv;
    }

    public List<ModelDescription.ScalarVariable> getInputs() {
        return inputs;
    }

    public List<ModelDescription.ScalarVariable> getOutputs() {
        return outputs;
    }

    @Override
    public void load() throws FmuInvocationException, FmuMissingLibraryException {

    }

    @Override
    public IFmiComponent instantiate(String s, String s1, boolean b, boolean b1, IFmuCallback iFmuCallback) throws XPathExpressionException, FmiInvalidNativeStateException {
        this.environmentFMUComponent = new EnvironmentFMUComponent(this, inputs, outputs);
        return this.environmentFMUComponent;
    }

    @Override
    public void unLoad() throws FmiInvalidNativeStateException {

    }

    @Override
    public String getVersion() throws FmiInvalidNativeStateException {
        return "2.0";
    }

    @Override
    public String getTypesPlatform() throws FmiInvalidNativeStateException {
        return "";
    }

    @Override
    public InputStream getModelDescription() throws ZipException, IOException {
        return new ByteArrayInputStream(this.modelDescriptionXML.getBytes());

    }

    @Override
    public boolean isValid() {
        return false;
    }


    /**
     * The valuereference start index is equal to
     *
     * @param envInputs
     */
    public void calculateInputs(HashMap<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envInputs) {
        calculateScalarVariable(envInputs, IOTYPE.INPUT);

    }

    private void calculateScalarVariable(Map<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envInputs, IOTYPE IO) {
        for (Map.Entry<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> entry : envInputs.entrySet()) {
            for (ModelDescription.ScalarVariable sv : entry.getValue()) {
                ModelDescription.ScalarVariable newEnvSv = null;
                switch (IO) {
                    case INPUT:
                        newEnvSv = createInputScalarVariable(sv, nextValueReference);
                        this.inputs.add(newEnvSv);
                        break;
                    case OUTPUT:
                        newEnvSv = createOutputScalarVariable(sv, nextValueReference);
                        this.outputs.add(newEnvSv);
                        break;
                }
                this.sourceToEnvironmentVariable.put(new ModelConnection.Variable(entry.getKey(), sv.name), newEnvSv);
                nextValueReference++;
            }
        }
    }

    public void calculateOutputs(Map<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envOutputs) {
        calculateScalarVariable(envOutputs, IOTYPE.OUTPUT);
    }
}
