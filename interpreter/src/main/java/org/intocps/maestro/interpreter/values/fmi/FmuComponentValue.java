package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.IFmiComponent;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

public class FmuComponentValue extends ExternalModuleValue<IFmiComponent> {
    public FmuComponentValue(Map<String, Value> members, IFmiComponent module) {
        super(members, module);
    }
}
