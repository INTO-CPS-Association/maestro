package org.intocps.maestro.interpreter.values;

public class UpdatableValue extends Value {

    private Value value;

    public UpdatableValue(Value value) {
        this.value = value;
    }

    @Override
    public Value deref() {
        return value.deref();
    }


    public void setValue(Value newValue) {
        this.value = newValue;
    }
}
