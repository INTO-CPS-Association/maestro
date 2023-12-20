package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.jnifmuapi.fmi3.IFmi3Instance;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.io.OutputStream;
import java.util.Map;

public class Fmu3InstanceValue extends ExternalModuleValue<IFmi3Instance> {
    final OutputStream fmuLoggerOutputStream;
    private final String name;

    public Fmu3InstanceValue(Map<String, Value> members, IFmi3Instance module, String name, OutputStream fmuLoggerOutputStream) {
        super(members, module);
        this.fmuLoggerOutputStream = fmuLoggerOutputStream;
        this.name = name;
    }

    public OutputStream getFmuLoggerOutputStream() {
        return fmuLoggerOutputStream;
    }

    @Override
    public String toString() {
        return name;
    }
}
