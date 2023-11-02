package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import java.util.HashMap;
import java.util.Map;

public class PortValueExpresssionMapImpl<PS> extends HashMap<FmiBuilder.Port<PS, PStm>, FmiBuilder.ExpressionValue> implements FmiBuilder.Fmi2ComponentVariable.PortExpressionValueMap<PS, PStm> {
    public PortValueExpresssionMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PortValueExpresssionMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public PortValueExpresssionMapImpl() {
    }

    public PortValueExpresssionMapImpl(Map<? extends FmiBuilder.Port<PS, PStm>, ? extends FmiBuilder.ExpressionValue> m) {
        super(m);
    }
}
