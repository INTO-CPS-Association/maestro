package org.intocps.orchestration.coe.webapi.services;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
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
    final ModelConnection.ModelInstance environmentFmuModelInstance;
    final String fmuName;
    // A map from the non-virtual variable to the corresponding virtual scalar variable in the environment FMU.
    private final Map<String, ModelDescription.ScalarVariable> sourceToEnvironmentVariableInputs = new HashMap<>();
    private final Map<String, ModelDescription.ScalarVariable> sourceToEnvironmentVariableOutputs = new HashMap<>();
    // The inputs of the environment FMU
    private final List<ModelDescription.ScalarVariable> inputs = new ArrayList<>();
    // The outputs of the environment FMU
    private final List<ModelDescription.ScalarVariable> outputs = new ArrayList<>();
    private Integer nextValueReference = 1;
    private String modelDescriptionXML;
    private EnvironmentFMUComponent environmentFMUComponent;

    public EnvironmentFMU(String fmuName, String instanceName) {
        this.fmuName = fmuName;
        this.environmentFmuModelInstance = new ModelConnection.ModelInstance("{" + fmuName + "}", instanceName);
    }

    public static EnvironmentFMU getInstance() {

        return environmentFMU;
    }

    public static EnvironmentFMU CreateEnvironmentFMU(String fmuName, String instanceName) {
        environmentFMU = new EnvironmentFMU(fmuName, instanceName);
        return getInstance();
    }

    /**
     * Gets the value of the inputs of the environment FMU.
     * The requested outputs from the non-virtual FMUs corresponding to the inputs of the environment FMU.
     *
     * @return
     */
    // All the requested outputs are outputs from the non-virtual FMUs and inputs to the virtual environment FMU.
    // Therefore, all the inputs of environment FMU originates from requested outputs.
    public Map<ModelConnection.Variable, Object> getRequestedOutputValues() throws InvalidVariableStringException {
        Map<ModelConnection.Variable, Object> requestedOutputValues = new HashMap<>();
        for (Map.Entry<String, ModelDescription.ScalarVariable> entry :
                sourceToEnvironmentVariableInputs.entrySet()) {
            requestedOutputValues.put(ModelConnection.Variable.parse(entry.getKey()), this.environmentFMUComponent.getValue(entry.getValue()));
        }

        return requestedOutputValues;
    }

    /**
     * Sets the value of the outputs of the environment FMU.
     *
     * @param outputValues
     */
    public void setOutputValues(List<ModelParameter> outputValues) {
        outputValues.forEach(output -> environmentFMUComponent.setOutput(sourceToEnvironmentVariableOutputs.get(output.variable.toString()), output.value));

    }

    public Map<String, ModelDescription.ScalarVariable> getSourceToEnvironmentVariableInputs() {
        return sourceToEnvironmentVariableInputs;
    }

    public Map<String, ModelDescription.ScalarVariable> getSourceToEnvironmentVariableOutputs() {
        return sourceToEnvironmentVariableOutputs;
    }

    public ModelConnection.Variable createVariable(ModelDescription.ScalarVariable sv) throws InvalidVariableStringException {
        return ModelConnection.Variable.parse(this.environmentFmuModelInstance.toString() + "." + sv.name);
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

    private ModelDescription.ScalarVariable createOutputScalarVariable(ModelDescription.ScalarVariable sourceScalarVariable, long valueReference) {
        ModelDescription.ScalarVariable outputScalarVariable = new ModelDescription.ScalarVariable();
        outputScalarVariable.causality = ModelDescription.Causality.Output;
        outputScalarVariable.variability = ModelDescription.Variability.Discrete;
        outputScalarVariable.type = new ModelDescription.Type();
        outputScalarVariable.type.type = sourceScalarVariable.type.type;
        if (sourceScalarVariable.type.start != null) {
            outputScalarVariable.type.start = sourceScalarVariable.type.start;
            outputScalarVariable.initial = ModelDescription.Initial.Exact;
        }
        outputScalarVariable.valueReference = valueReference;

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
                        this.sourceToEnvironmentVariableInputs.put(new ModelConnection.Variable(entry.getKey(), sv.name).toString(), newEnvSv);
                        break;
                    case OUTPUT:
                        newEnvSv = createOutputScalarVariable(sv, nextValueReference);
                        this.outputs.add(newEnvSv);
                        this.sourceToEnvironmentVariableOutputs.put(new ModelConnection.Variable(entry.getKey(), sv.name).toString(), newEnvSv);
                        break;
                }
                nextValueReference++;
            }
        }
    }

    public void calculateOutputs(Map<ModelConnection.ModelInstance, List<ModelDescription.ScalarVariable>> envOutputs) {
        calculateScalarVariable(envOutputs, IOTYPE.OUTPUT);
    }


}
