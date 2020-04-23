package org.intocps.maestro.interpreter.values;

public class StringValue extends Value {

    final String value;

    public String getValue() {
        return value;
    }

    public StringValue(String value) {
        this.value = value;
    }
}
