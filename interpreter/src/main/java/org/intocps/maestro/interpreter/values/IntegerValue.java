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
        return "" + value;
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
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public int compareTo(Value other) {
        other = other.deref();

        if (other instanceof IntegerValue || other instanceof ByteValue || other instanceof ShortValue) {
            NumericValue io = (NumericValue) other;
            return (value < io.intValue() ? -1 : (value == io.intValue() ? 0 : 1));
        }

        return super.compareTo(other);
    }
}
