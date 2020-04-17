package org.intocps.maestro.interpreter.values;

public class BooleanValue extends Value {
    final Boolean value;

    public Boolean getValue() {
        return value;
    }

    public BooleanValue(Boolean value) {
        this.value = value;
    }
}
