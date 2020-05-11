package org.intocps.maestro.multimodelparser;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class Port
{
    public final ModelConnection.ModelInstance modelInstance;
    public final ModelDescription.ScalarVariable sv;

    public Port(ModelConnection.ModelInstance modelInstance, ModelDescription.ScalarVariable sv)
    {
        this.modelInstance = modelInstance;
        this.sv = sv;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj instanceof org.intocps.orchestration.coe.initializing.Port)
            return this.modelInstance.equals(((org.intocps.orchestration.coe.initializing.Port) obj).modelInstance)
                    && sv.equals(((org.intocps.orchestration.coe.initializing.Port) obj).sv);
        return false;
    }

    @Override public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(modelInstance);
        builder.append(sv);
        return builder.hashCode();
    }

    @Override public String toString()
    {
        return modelInstance.toString() + "." + sv.name + ": "
                + sv.causality;
    }
}
