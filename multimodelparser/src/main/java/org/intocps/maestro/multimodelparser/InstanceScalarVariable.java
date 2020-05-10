package org.intocps.maestro.multimodelparser;

import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class InstanceScalarVariable {
    private ModelConnection.ModelInstance instance;
    private ModelDescription.ScalarVariable scalarVariable;
    public InstanceScalarVariable(ModelConnection.ModelInstance instance, ModelDescription.ScalarVariable scalarVariable) {
        this.instance = instance;
        this.scalarVariable = scalarVariable;
    }
}
