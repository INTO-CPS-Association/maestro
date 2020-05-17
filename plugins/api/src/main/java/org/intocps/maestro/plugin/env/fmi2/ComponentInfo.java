package org.intocps.maestro.plugin.env.fmi2;

import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public class ComponentInfo implements UnitRelationship.FrameworkUnitInfo {
    public final ModelDescription modelDescription;
    public final String fmuIdentifier;

    public ComponentInfo(ModelDescription modelDescription, String fmuIdentifier) {
        this.modelDescription = modelDescription;
        this.fmuIdentifier = fmuIdentifier;
    }
}
