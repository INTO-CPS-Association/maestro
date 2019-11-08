package org.intocps.orchestration.coe.webapi.services;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class EnvironmentFMU implements IFmu {
    private static EnvironmentFMU environmentFMU;
    final String key;
    final String fmuName;
    final String instanceName;
    private String modelDescriptionXML;
    private List<ModelDescription.ScalarVariable> inputs;
    private List<ModelDescription.ScalarVariable> outputs;
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
    public void calculateInputs(List<ModelDescription.ScalarVariable> outputs) {
        this.inputs = outputs.stream().map(sv -> {
            ModelDescription.ScalarVariable inpSv = new ModelDescription.ScalarVariable();
            inpSv.causality = ModelDescription.Causality.Input;
            inpSv.type = new ModelDescription.Type();
            inpSv.type.type = sv.type.type;
            inpSv.variability = ModelDescription.Variability.Discrete;
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
            outputScalarVariable.variability = ModelDescription.Variability.Discrete;
            outputScalarVariable.type = new ModelDescription.Type();
            outputScalarVariable.type.type = scalarVariable.type.type;
            outputScalarVariable.valueReference = scalarVariable.valueReference;
            outputScalarVariable.name = scalarVariable.name;
            return outputScalarVariable;
        }).collect(Collectors.toList());
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
}
