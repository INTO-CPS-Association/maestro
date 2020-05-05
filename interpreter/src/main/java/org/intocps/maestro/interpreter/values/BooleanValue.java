package org.intocps.maestro.interpreter.values;

public class BooleanValue extends Value {
    final Boolean value;

    public BooleanValue(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "BooleanValue{" + "value=" + value + '}';
    }
}
