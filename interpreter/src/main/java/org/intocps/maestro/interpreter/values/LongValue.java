package org.intocps.maestro.interpreter.values;

public class LongValue extends NumericValue {
    final long value;

    public LongValue(long value) {
        this.value = value;
    }

    public long getValue() {
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
        return Long.valueOf(value).intValue();
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

        if (other instanceof LongValue) {
            LongValue io = (LongValue) other;
            return (value < io.value ? -1 : (value == io.value ? 0 : 1));
        }

        return super.compareTo(other);
    }
}
