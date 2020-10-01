package org.intocps.maestro.interpreter.values;

public class NullValue extends Value {

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public int compareTo(Value value) {
        if (value instanceof NullValue) {
            return 0;
        }
        throw new ClassCastException();
    }
}
