package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.HashMap;
import java.util.Map;

public class PortValueExpresssionMapImpl<PS> extends HashMap<Fmi2Builder.Port<PS>, Fmi2Builder.ExpressionValue> implements Fmi2Builder.Fmi2ComponentVariable.PortExpressionValueMap<PS> {
    public PortValueExpresssionMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortValueExpresssionMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortValueExpresssionMapImpl() {
    }

    public PortValueExpresssionMapImpl(Map<? extends Fmi2Builder.Port<PS>, ? extends Fmi2Builder.ExpressionValue> m) {
        super(m);
    }
}
