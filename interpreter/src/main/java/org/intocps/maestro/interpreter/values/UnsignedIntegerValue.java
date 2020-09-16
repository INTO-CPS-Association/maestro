package org.intocps.maestro.interpreter.values;

public class UnsignedIntegerValue extends NumericValue {
    final long value;

    public UnsignedIntegerValue(long value) {
        this.value = value;
    }

    public long getValue() {
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
        return Math.toIntExact(value);
    }

    @Override
    public double realValue() {
        return value;
    }

    @Override
    public int compareTo(Value other) {
        other = other.deref();

        if (other instanceof UnsignedIntegerValue) {
            UnsignedIntegerValue io = (UnsignedIntegerValue) other;
            return (value < io.value ? -1 : (value == io.value ? 0 : 1));
        }

        return super.compareTo(other);
    }
}
