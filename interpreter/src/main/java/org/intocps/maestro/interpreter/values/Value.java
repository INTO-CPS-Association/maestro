package org.intocps.maestro.interpreter.values;

public abstract class Value {

    public int compareTo(Value value) {
        throw new RuntimeException("not applicable: " + this + " compare to " + value);
    }

    public Value deref() {
        return this;
    }
}
