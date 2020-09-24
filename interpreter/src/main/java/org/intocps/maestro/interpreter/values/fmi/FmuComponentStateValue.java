package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.IFmiComponentState;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;

import java.util.HashMap;

public class FmuComponentStateValue extends ExternalModuleValue<IFmiComponentState> {


    public FmuComponentStateValue(IFmiComponentState module) {
        super(new HashMap<>(), module);
    }
}
