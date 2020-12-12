package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;

import java.util.List;
import java.util.Objects;

public abstract class Value {

    public static void checkArgLength(List<Value> values, int size) {
        if (values == null) {
            throw new InterpreterException("No values passed");
        }

        if (values.stream().anyMatch(Objects::isNull)) {
            throw new InterpreterException("Argument list contains null values");
        }

        if (values.size() != size) {
            if (values.size() < size) {
                throw new InterpreterException("Too few arguments");
            } else {
                throw new InterpreterException("Too many arguments");
            }
        }
    }

    public int compareTo(Value value) {
        throw new RuntimeException("not applicable: " + this + " compare to " + value);
    }

    public Value deref() {
        return this;
    }
}
