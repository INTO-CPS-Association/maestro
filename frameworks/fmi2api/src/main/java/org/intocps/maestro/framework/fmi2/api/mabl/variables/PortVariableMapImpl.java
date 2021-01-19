package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.HashMap;
import java.util.Map;

public class PortVariableMapImpl<V> extends HashMap<Fmi2Builder.Port, Fmi2Builder.Variable<PStm, V>> implements Fmi2Builder.Fmi2ComponentVariable.PortVariableMap<PStm, V> {
    public PortVariableMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortVariableMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortVariableMapImpl() {
    }

    public PortVariableMapImpl(Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.Variable<PStm, V>> m) {
        super(m);
    }
}
