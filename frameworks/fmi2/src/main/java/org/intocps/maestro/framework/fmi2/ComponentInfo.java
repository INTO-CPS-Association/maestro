package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class ComponentInfo implements FrameworkUnitInfo {
    public final ModelDescription modelDescription;
    public final String fmuIdentifier;

    public ComponentInfo(ModelDescription modelDescription, String fmuIdentifier) {
        this.modelDescription = modelDescription;
        this.fmuIdentifier = fmuIdentifier;
    }

    public ModelDescription getModelDescription() {
        return modelDescription;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}
