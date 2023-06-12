package org.intocps.maestro.interpreter.extensions.fmi3;

import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

class Fmi3InstanceArgMapping implements IArgMapping {
    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public long[] getLimits() {
        return null;
    }

    @Override
    public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
        return ExternalReflectCallHelper.ArgMapping.InOut.Input;
    }

    @Override
    public void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction) {

    }

    @Override
    public Object map(Value v) {
        if (v instanceof ExternalModuleValue) {
            return ((ExternalModuleValue<?>) v).getModule();
        }
        return null;
    }

    @Override
    public void mapOut(Value original, Object value) {
        throw new RuntimeException("This is only for input so should not be called");
    }

    @Override
    public Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn) {
        return new ExternalModuleValue<>(null, value) {};
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String getDescriptiveName() {
        return "FMI3Instance";
    }

    @Override
    public String getDefaultTestValue() {
        return "null";
    }
}
