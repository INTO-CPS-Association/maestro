package org.intocps.maestro.interpreter.values.utilities;

import org.intocps.maestro.interpreter.values.ArrayValue;
import org.intocps.maestro.interpreter.values.UpdatableValue;
import org.intocps.maestro.interpreter.values.Value;

public class ArrayUpdatableValue extends UpdatableValue {
    private final ArrayValue<Value> owner;
    private final int index;

    public ArrayUpdatableValue(ArrayValue<Value> owner, int index) {
        super(owner);
        this.owner = owner;
        this.index = index;
    }

    @Override
    public void setValue(Value newValue) {
        if (index >= owner.getValues().size()) {
            throw new RuntimeException("Out of bounds for array");
        }
        if (owner.getValues().get(index).deref() instanceof ArrayValue) {
            throw new RuntimeException("Assigning array to nested array is not allowed.");
        } else {
            owner.getValues().set(index, newValue);
        }
    }

    @Override
    public Value deref() {
        return owner.getValues().get(index).deref();
    }
}
