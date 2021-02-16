package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.interpreter.values.Value;

import java.util.UUID;

public class VariableStepConfigValue extends Value {

    private final UUID uuid;

    public VariableStepConfigValue(UUID uuid) {
        this.uuid = uuid;
    }


    public UUID getUuid() {
        return uuid;
    }
}
