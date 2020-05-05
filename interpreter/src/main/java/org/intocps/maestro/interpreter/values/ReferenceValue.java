package org.intocps.maestro.interpreter.values;

public class ReferenceValue extends Value {

    Value value;

    public ReferenceValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public Value deref() {
        return this.value;
    }

    @Override
    public String toString() {
        return "ReferenceValue{" + "value=" + value + '}';
    }
}
