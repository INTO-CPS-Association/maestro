package org.intocps.maestro.interpreter.values.datawriter;

import org.intocps.maestro.interpreter.values.Value;

import java.util.List;
import java.util.UUID;

public interface IDataListener {

    void writeHeader(UUID uuid, List<String> headers);

    void writeDataPoint(UUID uuid, double time, List<Value> dataPoint);

    void close();
}
