package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

public class FmuValue extends ExternalModuleValue<IFmu> {
    public FmuValue(Map<String, Value> members, IFmu module) {
        super(members, module);
    }
}
