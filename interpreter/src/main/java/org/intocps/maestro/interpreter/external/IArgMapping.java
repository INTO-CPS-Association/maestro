package org.intocps.maestro.interpreter.external;

import org.intocps.maestro.interpreter.values.Value;

public interface IArgMapping {
    int getDimension();

    long[] getLimits();

    ExternalReflectCallHelper.ArgMapping.InOut getDirection();

    Object map(Value v);

    void mapOut(Value original, Object value);

    Value mapOut(Object value);

    Class getType();

    String getDescriptiveName();

    String getDefaultTestValue();
}
