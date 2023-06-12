package org.intocps.maestro.interpreter.extensions.fmi3;

import org.intocps.fmi.jnifmuapi.fmi3.Fmi3Status;
import org.intocps.fmi.jnifmuapi.fmi3.FmuResult;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

class Fmi3StatusArgMapping implements IArgMapping {
    public static Value status2IntValue(Fmi3Status res) {
        switch (res) {

            case OK:
                return new IntegerValue(0);
            case Warning:
                return new IntegerValue(1);
            case Discard:
                return new IntegerValue(2);
            case Error:
                return new IntegerValue(3);
            case Fatal:
                return new IntegerValue(4);
        }
        return null;
    }
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
        return ExternalReflectCallHelper.ArgMapping.InOut.OutputThroughReturn;
    }

    @Override
    public void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction) {

    }

    @Override
    public Object map(Value v) {
        return null;
    }

    @Override
    public void mapOut(Value original, Object value) {

    }

    @Override
    public Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn) {
        if (value instanceof Fmi3Status) {
            return status2IntValue((Fmi3Status) value);
        } else if (value instanceof FmuResult) {
            return status2IntValue(((FmuResult) value).status);
        }
        return null;
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String getDescriptiveName() {
        return "int";
    }

    @Override
    public String getDefaultTestValue() {
        return "0";
    }
}
