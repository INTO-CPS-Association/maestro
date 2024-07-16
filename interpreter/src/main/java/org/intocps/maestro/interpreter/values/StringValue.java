package org.intocps.maestro.interpreter.values;

public class StringValue extends Value {

    final String value;

    public StringValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(Value other) {

        other = other.deref();
        if (other instanceof StringValue) {
            return this.value.compareTo(((StringValue) other).value);
        }

        return super.compareTo(other);
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }

}
