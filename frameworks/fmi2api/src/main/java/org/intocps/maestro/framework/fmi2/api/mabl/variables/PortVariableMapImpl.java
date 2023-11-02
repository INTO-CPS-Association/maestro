package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import java.util.HashMap;
import java.util.Map;

public class PortVariableMapImpl<V, PS> extends HashMap<FmiBuilder.Port<PS,PStm>, FmiBuilder.Variable<PStm, V>> implements FmiBuilder.FmiSimulationInstance.PortVariableMap<V, PS,PStm> {
    public PortVariableMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortVariableMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortVariableMapImpl() {
    }

    public PortVariableMapImpl(Map<? extends FmiBuilder.Port<PS,PStm>, ? extends FmiBuilder.Variable<PStm, V>> m) {
        super(m);
    }
}
