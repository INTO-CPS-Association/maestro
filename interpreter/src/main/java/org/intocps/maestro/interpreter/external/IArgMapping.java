package org.intocps.maestro.interpreter.external;

import org.intocps.maestro.interpreter.values.Value;

import java.util.Map;

public interface IArgMapping {
    int getDimension();

    long[] getLimits();

    ExternalReflectCallHelper.ArgMapping.InOut getDirection();
    void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction);

    Object map(Value v);

    void mapOut(Value original, Object value);

    Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn);

    Class getType();

    String getDescriptiveName();

    String getDefaultTestValue();
}
