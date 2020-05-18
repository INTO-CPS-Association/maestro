package org.intocps.maestro.webapi.maestro2;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {
    List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionID = session.getUri().getRawPath().substring(session.getUri().getRawPath().lastIndexOf("/") + 1);
        Maestro2SimulationController.sessionController.addSocket(sessionID, session);
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionID = session.getUri().getRawPath().substring(session.getUri().getRawPath().lastIndexOf("/") + 1);
        Maestro2SimulationController.sessionController.removeSocket(sessionID);
        sessions.remove(session);
    }
}
