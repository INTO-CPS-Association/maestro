package org.intocps.maestro.plugin.InitializerWrapCoe.Spec;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import scalaz.Alpha;

public class VariableLocation {
    String variableId;
    ModelDescription.Types fmiType;
    public VariableLocation(String variableId, ModelDescription.Types fmiType) {
        this.variableId = variableId;
        this.fmiType = fmiType;
    }

    public String getVariableId() {
        return variableId;
    }

    public ModelDescription.Types getFmiType() {
        return this.fmiType;
    }
}
