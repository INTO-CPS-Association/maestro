package org.intocps.maestro.interpreter.values;

public class RealValue extends Value {
    final double value;

    public RealValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
