package org.intocps.maestro.multimodelparser;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.HashMap;
import java.util.Set;

public class OutputsAndInputsToOutputs {
    final HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputToOutputs;
    final HashMap<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs;

    public OutputsAndInputsToOutputs(HashMap<ModelConnection.ModelInstance, HashMap<ModelDescription.ScalarVariable, InstanceScalarVariable>> inputToOutputs, HashMap<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> outputs) {
        this.inputToOutputs = inputToOutputs;
        this.outputs = outputs;
    }
}
