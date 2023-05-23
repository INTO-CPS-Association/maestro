package org.intocps.maestro.interpreter.values.fmi;

import org.intocps.fmi.jnifmuapi.fmi3.Fmi3State;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;

import java.util.HashMap;

public class Fmu3StateValue extends ExternalModuleValue<Fmi3State> {


    public Fmu3StateValue(Fmi3State module) {
        super(new HashMap<>(), module);
    }
}
