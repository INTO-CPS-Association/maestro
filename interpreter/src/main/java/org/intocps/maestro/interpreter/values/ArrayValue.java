package org.intocps.maestro.interpreter.values;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayValue<T extends Value> extends Value {
    final List<T> values;

    public ArrayValue(List<T> values) {
        this.values = values;
    }

    public List<T> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.stream().map(Object::toString).collect(Collectors.joining(",", "[", "]"));
    }
}
