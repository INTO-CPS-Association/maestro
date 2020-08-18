package org.intocps.maestro.interpreter.values.datawriter;

import org.intocps.maestro.interpreter.values.Value;

import java.util.UUID;

public class DataWriterConfigValue extends Value {

    private final UUID uuid;

    public DataWriterConfigValue(UUID uuid) {
        this.uuid = uuid;
    }


    public UUID getUuid() {
        return uuid;
    }
}
