package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.HashMap;
import java.util.Map;

public class PortVariableMapImpl extends HashMap<Fmi2Builder.Port, Fmi2Builder.Variable> implements Fmi2Builder.Fmi2ComponentApi.PortVariableMap {
    public PortVariableMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortVariableMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortVariableMapImpl() {
    }

    public PortVariableMapImpl(Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.Variable> m) {
        super(m);
    }
}
