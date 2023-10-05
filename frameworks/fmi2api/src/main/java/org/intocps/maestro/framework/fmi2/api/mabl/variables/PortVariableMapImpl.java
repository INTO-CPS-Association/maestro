package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.HashMap;
import java.util.Map;

public class PortVariableMapImpl<V,PS> extends HashMap<Fmi2Builder.Port<PS>, Fmi2Builder.Variable<PStm, V>> implements Fmi2Builder.FmiSimulationInstance.PortVariableMap<PStm,
        V,PS> {
    public PortVariableMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortVariableMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortVariableMapImpl() {
    }

    public PortVariableMapImpl(Map<? extends Fmi2Builder.Port<PS>, ? extends Fmi2Builder.Variable<PStm, V>> m) {
        super(m);
    }
}
