package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.HashMap;
import java.util.Map;

public class PortValueMapImpl<V> extends HashMap<Fmi2Builder.Port, Fmi2Builder.Value<V>> implements Fmi2Builder.Fmi2ComponentVariable.PortValueMap<V> {
    public PortValueMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortValueMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortValueMapImpl() {
    }

    public PortValueMapImpl(Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.Value<V>> m) {
        super(m);
    }
}
