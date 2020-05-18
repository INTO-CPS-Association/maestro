package org.intocps.maestro.websocketsupport;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SessionToMessageDelete {
    Map<String, Consumer<String>> sessionToMessageDelegate = new HashMap<>();

    public void AddEntry(String sessionID, Consumer<String> function) {
        this.sessionToMessageDelegate.put(sessionID, function);
    }

    public Consumer<String> GetMessageDelegate(String sessionID) {
        return sessionToMessageDelegate.get(sessionID);
    }
}
