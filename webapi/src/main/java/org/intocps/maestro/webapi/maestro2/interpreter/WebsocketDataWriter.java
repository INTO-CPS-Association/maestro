package org.intocps.maestro.webapi.maestro2.interpreter;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.RealValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

public class WebsocketDataWriter implements IDataListener {


    private final HashMap<UUID, List<String>> printers = new HashMap<>();
    private final WebSocketSession webSocketSession;
    private final WebsocketValueConverter webSocketConverter;
    private WebSocketSenderValue webSocketSender;

    public WebsocketDataWriter(WebSocketSession ws) {
        this.webSocketSession = ws;
        this.webSocketConverter = new WebsocketValueConverter(ws);
    }

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        this.printers.put(uuid, headers);
        this.webSocketSender = new WebSocketSenderValue(this.webSocketSession);
        this.webSocketConverter.configure(headers);

    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {

        List<Object> data = new Vector<>();
        data.add(time);

        for (Value d : dataPoint) {
            if (d instanceof IntegerValue) {
                data.add(((IntegerValue) d).intValue());
            }
            if (d instanceof RealValue) {
                data.add(((RealValue) d).realValue());
            }
            if (d instanceof BooleanValue) {
                data.add(Boolean.valueOf(((BooleanValue) d).getValue()));
            }
        }

        this.webSocketConverter.update(time, data);
        this.webSocketConverter.send();
    }

    @Override
    public void close() {
        try {
            this.webSocketSession.close();
        } catch (IOException e) {
            throw new InterpreterException(e);
        }

    }
}
