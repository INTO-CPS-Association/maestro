package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.core.FaultInject;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;

import java.util.Optional;

public class InstanceInfo implements FrameworkUnitInfo {
    public final Fmi3ModelDescription modelDescription;
    public final String fmuIdentifier;
    public Optional<FaultInject> faultInject = Optional.empty();

    public InstanceInfo(Fmi3ModelDescription modelDescription, String fmuIdentifier) {
        this.modelDescription = modelDescription;
        this.fmuIdentifier = fmuIdentifier;
    }

    public Optional<FaultInject> getFaultInject() {
        return this.faultInject;
    }

    public void setFaultInject(String constraintId) {
        this.faultInject = Optional.of(new FaultInject(constraintId));
    }

    public Fmi3ModelDescription getModelDescription() {
        return modelDescription;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }

    @Override
    public String getOwnerIdentifier() {
        return getFmuIdentifier();
    }
}
