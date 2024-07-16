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
        return "" + value;
    }

    @Override
    public int compareTo(Value other) {

        other = other.deref();
        if (other instanceof BooleanValue) {
            return this.value.compareTo(((BooleanValue) other).value);
        }

        return super.compareTo(other);
    }
}
