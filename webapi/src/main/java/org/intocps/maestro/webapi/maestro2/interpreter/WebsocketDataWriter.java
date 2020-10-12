package org.intocps.maestro.webapi.maestro2.interpreter;

import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.RealValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.datawriter.DataListenerUtilities;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Only works for a single websocket.
 */
public class WebsocketDataWriter implements IDataListener {
    private final HashMap<UUID, WebsocketDataWriterInstance> instances = new HashMap<>();
    private final WebSocketSession webSocketSession;
    private final WebsocketValueConverter webSocketConverter;

    public WebsocketDataWriter(WebSocketSession ws) {
        this.webSocketSession = ws;
        this.webSocketConverter = new WebsocketValueConverter(ws);
    }

    public static List<String> calculateHeadersOfInterest(FmiSimulationEnvironment environment) {
        return environment.getLivestreamVariablesToLog().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(x -> entry.getKey() + "." + x)).collect(Collectors.toList());
    }

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        FmiSimulationEnvironment environment = (FmiSimulationEnvironment) DataStore.GetInstance().getSimulationEnvironment();
        List<String> hoi = calculateHeadersOfInterest(environment);
        List<Integer> ioi = DataListenerUtilities.indicesOfInterest(headers, hoi);
        WebsocketDataWriterInstance wdwi = new WebsocketDataWriterInstance(hoi, ioi);
        wdwi.interval = environment.getEnvironmentMessage().getLivelogInterval();
        this.instances.put(uuid, wdwi);
        this.webSocketConverter.configure(hoi);
    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {
        WebsocketDataWriterInstance instance = instances.get(uuid);
        if (instance.shallLog(time)) {
            instance.updateNextReportTime();
            List<Object> data = new Vector<>();
            for (Integer i : instance.indicesOfInterest) {
                Value d = dataPoint.get(i);

                if (d instanceof IntegerValue) {
                    data.add(((IntegerValue) d).intValue());
                }
                if (d instanceof RealValue) {
                    data.add(((RealValue) d).realValue());
                }
                if (d instanceof BooleanValue) {
                    data.add(Boolean.valueOf(((BooleanValue) d).getValue()) + "");
                }
            }

            this.webSocketConverter.update(time, data);
            this.webSocketConverter.send();
        }
    }

    @Override
    public void close() {
        try {
            this.webSocketSession.close();
        } catch (IOException e) {
            throw new InterpreterException(e);
        }
    }

    static class WebsocketDataWriterInstance {
        public final List<Integer> indicesOfInterest;
        public final List<String> headersOfInterest;
        public double nextReportTime = 0.0;
        public Double interval = 0.1;

        WebsocketDataWriterInstance(List<String> headersOfInterest, List<Integer> indicesOfInterest) {
            this.headersOfInterest = headersOfInterest;
            this.indicesOfInterest = indicesOfInterest;
        }

        public Boolean shallLog(Double time) {
            return time >= nextReportTime;
        }

        public void updateNextReportTime() {
            this.nextReportTime += this.interval;
        }


    }
}
