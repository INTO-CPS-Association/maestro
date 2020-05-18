package org.intocps.maestro.websocketsupport;

import java.util.Map;

public class LivestreamMessage {
    public Double time;
    public Map<String, Map<String, String>> data;

    public LivestreamMessage(Double time, Map<String, Map<String, String>> data) {
        this.time = time;
        this.data = data;
    }
}