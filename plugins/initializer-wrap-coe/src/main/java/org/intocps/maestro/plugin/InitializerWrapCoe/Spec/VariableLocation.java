package org.intocps.maestro.plugin.InitializerWrapCoe.Spec;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.HashMap;
import java.util.Map;

public class VariableLocation {
    public Map<ModelDescription.Types, String> typeMapping = new HashMap<>();
    String variableId;
    ModelDescription.Types fmiType;

    public VariableLocation(String variableId, ModelDescription.Types fmiType) {
        this.variableId = variableId;
        this.fmiType = fmiType;
    }

    public void setTypeRemapping(ModelDescription.Types type, String variableId) {
        if (!typeMapping.containsKey(type)) {
            typeMapping.put(type, variableId);
        }
    }

    public String getTypeMapping(ModelDescription.Types type) {
        return typeMapping.get(type);
    }

    public String getVariableId() {
        return variableId;
    }

    public ModelDescription.Types getFmiType() {
        return this.fmiType;
    }
}
