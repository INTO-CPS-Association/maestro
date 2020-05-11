package org.intocps.maestro.multimodelparser;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class InstanceScalarVariable {
    public final  ModelConnection.ModelInstance modelInstance;
    public final  ModelDescription.ScalarVariable scalarVariable;
    public InstanceScalarVariable(ModelConnection.ModelInstance modelInstance, ModelDescription.ScalarVariable scalarVariable) {
        this.modelInstance = modelInstance;
        this.scalarVariable = scalarVariable;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj instanceof org.intocps.orchestration.coe.initializing.Port)
            return this.modelInstance.equals(((org.intocps.orchestration.coe.initializing.Port) obj).modelInstance)
                    && scalarVariable.equals(((org.intocps.orchestration.coe.initializing.Port) obj).sv);
        return false;
    }

    @Override public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(modelInstance);
        builder.append(scalarVariable);
        return builder.hashCode();
    }

    @Override public String toString()
    {
        return modelInstance.toString() + "." + scalarVariable.name + ": "
                + scalarVariable.causality;
    }
}
