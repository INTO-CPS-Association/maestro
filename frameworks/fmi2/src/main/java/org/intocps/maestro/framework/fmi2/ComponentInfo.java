package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;

import java.util.Optional;

public class ComponentInfo implements FrameworkUnitInfo {
    public final Fmi2ModelDescription modelDescription;
    public final String fmuIdentifier;
    public Optional<FaultInject> faultInject = Optional.empty();

    public ComponentInfo(Fmi2ModelDescription modelDescription, String fmuIdentifier) {
        this.modelDescription = modelDescription;
        this.fmuIdentifier = fmuIdentifier;
    }

    public Optional<FaultInject> getFaultInject() {
        return this.faultInject;
    }

    public void setFaultInject(String constraintId) {
        this.faultInject = Optional.of(new FaultInject(constraintId));
    }

    public Fmi2ModelDescription getModelDescription() {
        return modelDescription;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}
