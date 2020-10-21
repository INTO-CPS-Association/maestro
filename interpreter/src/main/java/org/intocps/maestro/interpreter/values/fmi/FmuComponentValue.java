package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.IFmiComponent;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.io.OutputStream;
import java.util.Map;

public class FmuComponentValue extends ExternalModuleValue<IFmiComponent> {
    final OutputStream fmuLoggerOutputStream;

    public FmuComponentValue(Map<String, Value> members, IFmiComponent module, OutputStream fmuLoggerOutputStream) {
        super(members, module);
        this.fmuLoggerOutputStream = fmuLoggerOutputStream;
    }

    public OutputStream getFmuLoggerOutputStream() {
        return fmuLoggerOutputStream;
    }
}
