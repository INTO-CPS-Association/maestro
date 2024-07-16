package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

public class ValueFmi2Api<V> implements FmiBuilder.Value<V> {
    final V value;
    final PType type;
    private boolean primitive;

    public ValueFmi2Api(PType type, V value) {
        this.value = value;
        this.type = type;
        this.primitive = false;
    }

    public ValueFmi2Api(PType type, V value, boolean primitive) {
        this(type, value);
        this.primitive = primitive;
    }

    public PType getType() {
        return type;
    }

    @Override
    public V get() {
        return value;
    }

    public boolean isPrimitive() {
        return primitive;
    }
}
