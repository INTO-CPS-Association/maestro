package org.intocps.maestro.webapi.maestro2.interpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    Dto data = new Dto();
    List<String> names = new ArrayList<>();

    public WebsocketValueConverter(WebSocketSession ws) {
        this.ws = ws;
    }

    public void configure(List<String> names) {

        this.names = names;


    }

    public void update(double time, List<Object> updates) {

        this.data.time = time;
        for (int i = 0; i < updates.size(); i++) {

            String[] name = names.get(i).split("\\.");

            Map<String, Object> dataMap = this.data.data;

            for (int segement = 0; segement < name.length; segement++) {
                String s = name[segement];

                if (segement == name.length - 1) {
                    dataMap.put(s, updates.get(i));
                } else {
                    dataMap = (Map<String, Object>) dataMap.computeIfAbsent(s, k -> new HashMap<>());
                }
            }

        }
    }

    public void send() {
        try {
            String json = getJson();
            logger.info("Sending: {}", json);
            this.ws.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getJson() throws com.fasterxml.jackson.core.JsonProcessingException {
        return mapper.writeValueAsString(data);
    }

    class Dto {
        public double time;
        public Map<String, Object> data = new HashMap<>();
    }
}
