package org.intocps.maestro.webapi.services;

import org.intocps.fmi.*;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.InvalidVariableStringException;
import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.plugin.initializer.ModelParameter;

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
    private final Map<String, Fmi2ModelDescription.ScalarVariable> sourceToEnvironmentVariableInputs = new HashMap<>();
    private final Map<String, Fmi2ModelDescription.ScalarVariable> sourceToEnvironmentVariableOutputs = new HashMap<>();
    // The inputs of the environment FMU
    private final List<Fmi2ModelDescription.ScalarVariable> inputs = new ArrayList<>();
    // The outputs of the environment FMU
    private final List<Fmi2ModelDescription.ScalarVariable> outputs = new ArrayList<>();
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

    public static EnvironmentFMU createEnvironmentFMU(String fmuName, String instanceName) {
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
        for (Map.Entry<String, Fmi2ModelDescription.ScalarVariable> entry : sourceToEnvironmentVariableInputs.entrySet()) {
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
        outputValues.forEach(
                output -> environmentFMUComponent.setOutput(sourceToEnvironmentVariableOutputs.get(output.variable.toString()), output.value));

    }

    public Map<String, Fmi2ModelDescription.ScalarVariable> getSourceToEnvironmentVariableInputs() {
        return sourceToEnvironmentVariableInputs;
    }

    public Map<String, Fmi2ModelDescription.ScalarVariable> getSourceToEnvironmentVariableOutputs() {
        return sourceToEnvironmentVariableOutputs;
    }

    public ModelConnection.Variable createVariable(Fmi2ModelDescription.ScalarVariable sv) throws InvalidVariableStringException {
        return ModelConnection.Variable.parse(this.environmentFmuModelInstance.toString() + "." + sv.name);
    }


    public void createModelDescriptionXML() {
        modelDescriptionXML = EnvironmentFMUModelDescription.createEnvironmentFMUModelDescription(inputs, outputs, fmuName);
    }

    private Object start(Fmi2ModelDescription.Type type) {
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

    private Fmi2ModelDescription.ScalarVariable createOutputScalarVariable(Fmi2ModelDescription.ScalarVariable sourceScalarVariable, long valueReference) {
        Fmi2ModelDescription.ScalarVariable outputScalarVariable = new Fmi2ModelDescription.ScalarVariable();
        outputScalarVariable.causality = Fmi2ModelDescription.Causality.Output;
        outputScalarVariable.variability = Fmi2ModelDescription.Variability.Discrete;
        outputScalarVariable.type = new Fmi2ModelDescription.Type();
        outputScalarVariable.type.type = sourceScalarVariable.type.type;
        if (sourceScalarVariable.type.start != null) {
            outputScalarVariable.type.start = sourceScalarVariable.type.start;
            outputScalarVariable.initial = Fmi2ModelDescription.Initial.Exact;
        }
        outputScalarVariable.valueReference = valueReference;

        // valueReference added to name to ensure uniqueness
        outputScalarVariable.name = sourceScalarVariable.name + valueReference;
        return outputScalarVariable;
    }

    private Fmi2ModelDescription.ScalarVariable createInputScalarVariable(Fmi2ModelDescription.ScalarVariable sourceScalarVariable, long valueReference) {
        Fmi2ModelDescription.ScalarVariable inpSv = new Fmi2ModelDescription.ScalarVariable();
        inpSv.causality = Fmi2ModelDescription.Causality.Input;
        inpSv.type = new Fmi2ModelDescription.Type();
        inpSv.type.type = sourceScalarVariable.type.type;
        inpSv.variability = Fmi2ModelDescription.Variability.Discrete;
        inpSv.type.start = start(sourceScalarVariable.type);
        inpSv.valueReference = valueReference;
        // valueReference added to name to ensure uniqueness
        inpSv.name = sourceScalarVariable.name + valueReference;
        return inpSv;
    }

    public List<Fmi2ModelDescription.ScalarVariable> getInputs() {
        return inputs;
    }

    public List<Fmi2ModelDescription.ScalarVariable> getOutputs() {
        return outputs;
    }

    @Override
    public void load() throws FmuInvocationException, FmuMissingLibraryException {

    }

    @Override
    public IFmiComponent instantiate(String s, String s1, boolean b, boolean b1,
            IFmuCallback iFmuCallback) throws XPathExpressionException, FmiInvalidNativeStateException {
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
    public void calculateInputs(HashMap<ModelConnection.ModelInstance, List<Fmi2ModelDescription.ScalarVariable>> envInputs) {
        calculateScalarVariable(envInputs, IOTYPE.INPUT);
    }

    private void calculateScalarVariable(Map<ModelConnection.ModelInstance, List<Fmi2ModelDescription.ScalarVariable>> envInputs, IOTYPE IO) {
        for (Map.Entry<ModelConnection.ModelInstance, List<Fmi2ModelDescription.ScalarVariable>> entry : envInputs.entrySet()) {
            for (Fmi2ModelDescription.ScalarVariable sv : entry.getValue()) {
                Fmi2ModelDescription.ScalarVariable newEnvSv = null;
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

    public void calculateOutputs(Map<ModelConnection.ModelInstance, List<Fmi2ModelDescription.ScalarVariable>> envOutputs) {
        calculateScalarVariable(envOutputs, IOTYPE.OUTPUT);
    }


}
