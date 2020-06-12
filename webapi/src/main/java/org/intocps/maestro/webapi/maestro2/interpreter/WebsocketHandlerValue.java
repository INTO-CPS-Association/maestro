package org.intocps.maestro.webapi.maestro2.interpreter;

import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.FunctionValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.VoidValue;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

class WebsocketHandlerValue extends ExternalModuleValue<WebSocketSession> {
    public WebsocketHandlerValue(WebSocketSession ws) {
        super(createMembers(ws), ws);
    }

    private static Map<String, Value> createMembers(WebSocketSession ws) {
        Map<String, Value> members = new HashMap<>();

        members.put("open", new FunctionValue.ExternalFunctionValue(fcargs -> new WebSocketSenderValue(ws)));
        members.put("close", new FunctionValue.ExternalFunctionValue(fcargs -> new VoidValue()));

        return members;
    }
}
