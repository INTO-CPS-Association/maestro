package org.intocps.maestro.interpreter.values.datawriter;

import org.intocps.maestro.interpreter.values.Value;

import java.util.List;
import java.util.UUID;

public class IntervalDataWriter implements IDataListener {
    final double interval;
    double nextUpdateTime = 0;
    final IDataListener listener;

    public IntervalDataWriter(double interval, IDataListener listener) {
        this.interval = interval;
        this.listener = listener;
    }

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        listener.writeHeader(uuid, headers);
    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {
        if (time >= nextUpdateTime) {
            nextUpdateTime += this.interval;
            listener.writeDataPoint(uuid, time, dataPoint);
        }
    }

    @Override
    public void close() {
        listener.close();

    }
}
