package org.intocps.maestro.interpreter.values;

public class IntegerValue extends Value {
    final int value;

    public IntegerValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "IntegerValue{" + "value=" + value + '}';
    }
}
