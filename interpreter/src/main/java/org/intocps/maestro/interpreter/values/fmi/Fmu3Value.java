package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.jnifmuapi.fmi3.IFmi3Fmu;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

public class Fmu3Value extends ExternalModuleValue<IFmi3Fmu> {
    public Fmu3Value(Map<String, Value> members, IFmi3Fmu module) {
        super(members, module);
    }
}