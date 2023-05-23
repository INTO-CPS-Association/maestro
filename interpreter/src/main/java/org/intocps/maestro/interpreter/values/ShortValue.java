package org.intocps.maestro.interpreter.values;

public class ShortValue extends NumericValue {
    final short value;

    public ShortValue(short value) {
        this.value = value;
    }

    public short getValue() {
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

        if (other instanceof ShortValue) {
            ShortValue io = (ShortValue) other;
            return Short.compare(value, io.value);
        } else if (other instanceof IntegerValue) {
            IntegerValue io = (IntegerValue) other;
            return (value < io.value ? -1 : (value == io.value ? 0 : 1));
        }

        return super.compareTo(other);
    }
}