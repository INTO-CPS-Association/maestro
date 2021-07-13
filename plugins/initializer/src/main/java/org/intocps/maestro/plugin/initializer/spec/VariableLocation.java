package org.intocps.maestro.plugin.initializer.spec;

import org.intocps.maestro.fmi.Fmi2ModelDescription;

import java.util.HashMap;
import java.util.Map;

public class VariableLocation {
    public Map<Fmi2ModelDescription.Types, String> typeMapping = new HashMap<>();
    String variableId;
    Fmi2ModelDescription.Types fmiType;

    public VariableLocation(String variableId, Fmi2ModelDescription.Types fmiType) {
        this.variableId = variableId;
        this.fmiType = fmiType;
    }

    public void setTypeRemapping(Fmi2ModelDescription.Types type, String variableId) {
        if (!typeMapping.containsKey(type)) {
            typeMapping.put(type, variableId);
        }
    }

    public String getTypeMapping(Fmi2ModelDescription.Types type) {
        return typeMapping.get(type);
    }

    public String getVariableId() {
        return variableId;
    }

    public Fmi2ModelDescription.Types getFmiType() {
        return this.fmiType;
    }
}
