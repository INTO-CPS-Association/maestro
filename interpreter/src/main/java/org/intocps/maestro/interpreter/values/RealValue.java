package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;

public class RealValue extends NumericValue {
    final double value;

    public RealValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public long longValue() {
        long rounded = Math.round(value);

        if (rounded != value) {
            throw new InterpreterException("Value " + value + " is not an integer");
        }

        return rounded;
    }

    @Override
    public int intValue() {
        long rounded = Math.round(value);

        if (rounded != value) {
            throw new InterpreterException("Value " + value + " is not an integer");
        }

        if (rounded <= Integer.MAX_VALUE && rounded >= Integer.MIN_VALUE) {
            return (int) rounded; // No loss of precision
        } else {
            throw new InterpreterException("Cannot convert " + rounded + " to int");
        }
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
        return (float) value;
    }

    @Override
    public int compareTo(Value value) {

        Value other = value.deref();

        if (other instanceof RealValue) {
            RealValue ro = (RealValue) other;
            return (int) Math.round(Math.signum(this.value - ro.getValue()));

        } else if (other instanceof NumericValue) {
            NumericValue ro = (NumericValue) other;
            return (int) Math.round(Math.signum(this.value - ro.intValue()));
        }

        return super.compareTo(value);
    }

    @Override
    public boolean isNumericDecimal() {
        return true;
    }

}
