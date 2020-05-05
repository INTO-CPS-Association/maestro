package org.intocps.maestro.interpreter.values;

public class IntegerValue extends NumericValue {
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

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public double realValue() {
        return value;
    }

    @Override
    public int compareTo(Value other) {
        other = other.deref();

        if (other instanceof IntegerValue) {
            IntegerValue io = (IntegerValue) other;
            return (value < io.value ? -1 : (value == io.value ? 0 : 1));
        }

        return super.compareTo(other);
    }
}
