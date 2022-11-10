package org.intocps.maestro.interpreter.values;

public class ByteValue extends NumericValue {
    final byte value;

    public ByteValue(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ByteValue{" + "value=" + value + '}';
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

        if (other instanceof ByteValue) {
            ByteValue io = (ByteValue) other;
            return Byte.compare(value, io.value);
        } else if (other instanceof IntegerValue) {
            IntegerValue io = (IntegerValue) other;
            return (value < io.value ? -1 : (value == io.value ? 0 : 1));
        }

        return super.compareTo(other);
    }
}