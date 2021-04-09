package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;

import java.util.Optional;

public class ComponentInfo implements FrameworkUnitInfo {
    public final ModelDescription modelDescription;
    public final String fmuIdentifier;
    public Optional<FaultInject> faultInject = Optional.empty();

    public ComponentInfo(ModelDescription modelDescription, String fmuIdentifier) {
        this.modelDescription = modelDescription;
        this.fmuIdentifier = fmuIdentifier;
    }

    public Optional<FaultInject> getFaultInject() {
        return this.faultInject;
    }

    public void setFaultInject(String constraintId) {
        this.faultInject = Optional.of(new FaultInject(constraintId));
    }

    public ModelDescription getModelDescription() {
        return modelDescription;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}
