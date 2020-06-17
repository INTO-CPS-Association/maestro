package org.intocps.maestro.webapi.maestro2.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.VoidValue;
import org.springframework.web.socket.WebSocketSession;

public class WebApiInterpreterFactory extends DefaultExternalValueFactory {

    public WebApiInterpreterFactory(WebSocketSession ws) {
        super();
        instantiators.put("WebsocketHandler", args -> {
            if (ws == null) {
                return Either.left(new AnalysisException("No websocket present"));
            } else {
                return Either.right(new WebsocketHandlerValue(ws));
            }
        });
    }

    @Override
    public Value destroy(Value value) {
        if (value instanceof WebsocketHandlerValue) {
            return new VoidValue();
        }
        return super.destroy(value);
    }

}
