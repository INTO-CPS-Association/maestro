package org.intocps.maestro.interpreter.values;

public class UpdatableValue extends Value {

    private Value value;

    public UpdatableValue(Value value) {
        this.value = value;
    }

    @Override
    public Value deref() {
        return value.deref();
    }


    public void setValue(Value newValue) {
        if (this.value instanceof ArrayValue) {
            ArrayValue valueAsArray = (ArrayValue) this.value;
            if (newValue instanceof ArrayValue) {
                ArrayValue newValueAsArray = (ArrayValue) newValue;
                for (int i = 0; i < ((ArrayValue) newValue).getValues().size(); i++) {
                    Object newValueForIndex = newValueAsArray.getValues().get(i);
                    valueAsArray.getValues().set(i, newValueForIndex);
                }
            }

        } else {
            this.value = newValue;
        }
    }
}
