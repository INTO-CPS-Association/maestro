package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import java.util.HashMap;
import java.util.Map;

public class PortValueMapImpl<V, PS> extends HashMap<FmiBuilder.Port<PS>, FmiBuilder.Value<V>> implements FmiBuilder.Fmi2ComponentVariable.PortValueMap<V, PS> {
    public PortValueMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortValueMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortValueMapImpl() {
    }

    public PortValueMapImpl(Map<? extends FmiBuilder.Port<PS>, ? extends FmiBuilder.Value<V>> m) {
        super(m);
    }
}
