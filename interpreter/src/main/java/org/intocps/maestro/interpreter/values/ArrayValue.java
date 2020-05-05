package org.intocps.maestro.interpreter.values;

import java.util.List;

public class ArrayValue<T extends Value> extends Value {
    final List<T> values;

    public ArrayValue(List<T> values) {
        this.values = values;
    }

    public List<T> getValues() {
        return values;
    }
}
