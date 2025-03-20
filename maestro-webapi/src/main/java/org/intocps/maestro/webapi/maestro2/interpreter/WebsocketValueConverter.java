package org.intocps.maestro.webapi.maestro2.interpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.interpreter.values.datawriter.DataListenerUtilities;
import org.intocps.maestro.interpreter.values.datawriter.WebSocketDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebsocketValueConverter {

    final static ObjectMapper mapper = new ObjectMapper();
    private final static Logger logger = LoggerFactory.getLogger(WebsocketValueConverter.class);
    final WebSocketSession ws;
    final WebSocketDataWriter.JsonLiveStreamValueConverter converter = new WebSocketDataWriter.JsonLiveStreamValueConverter();

    public WebsocketValueConverter(WebSocketSession ws) {
        this.ws = ws;
    }

    public void configure(List<String> names) {
        configure(DataListenerUtilities.indicesToHeaders(names, null));
    }
    public void configure(Map<Integer, String> ith) {
     converter.configure(ith);
    }

    /**
     * Updates the variables to be send. It converts dottet string names into objects
     *
     * @param time
     * @param updates
     */
    public void update(double time, List<Object> updates) {
        converter.update(time, updates);
    }

    public void send() {
        try {
            String json = converter.getJson();
            logger.trace("Sending: {}", json);
            this.ws.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getJson() throws JsonProcessingException {
        return converter.getJson();
    }

}
