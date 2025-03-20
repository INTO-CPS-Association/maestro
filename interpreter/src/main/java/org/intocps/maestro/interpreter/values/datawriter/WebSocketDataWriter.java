package org.intocps.maestro.interpreter.values.datawriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.NumericValue;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketDataWriter implements IDataListener {
    final static Logger logger = LoggerFactory.getLogger(WebSocketDataWriter.class);
    final MinimalWebSocketServer server;
    final JsonLiveStreamValueConverter valueConverter = new JsonLiveStreamValueConverter();
    private final List<String> filter;

    static final String CSV_DATA_WRITER_PRECISION = "WEBSOCKET_DATA_WRITER_PRECISION";

    static final String floatFormatter = System.getProperty(CSV_DATA_WRITER_PRECISION) != null ? ("%." + System.getProperty(
            CSV_DATA_WRITER_PRECISION) + "f") : null;


    static public class JsonLiveStreamValueConverter {
        private Map<Integer, String> indexToHeader;
        final static ObjectMapper mapper = new ObjectMapper();
        private final static Logger logger = LoggerFactory.getLogger(JsonLiveStreamValueConverter.class);
        LiveStreamRootObject data = new LiveStreamRootObject();

        public void configure(Map<Integer, String> indexToHeader) {
            this.indexToHeader = indexToHeader;
        }

        /**
         * Updates the variables to be send. It converts dottet string names into objects
         *
         * @param time
         * @param updates
         */
        public void update(double time, List<Object> updates) {

            this.data.time = time;
            for (int i = 0; i < updates.size(); i++) {

                if (!indexToHeader.containsKey(i)) {
                    continue;
                }
                String[] name = indexToHeader.get(i).split("\\.");

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


        public String getJson() throws com.fasterxml.jackson.core.JsonProcessingException {
            return mapper.writeValueAsString(data);
        }

        private class LiveStreamRootObject {
            public double time;
            public Map<String, Object> data = new HashMap<>();
        }
    }

    public class MinimalWebSocketServer extends WebSocketServer {

        private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

        public MinimalWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            connections.add(conn);
            logger.debug("New connection: {}", conn.getRemoteSocketAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            connections.remove(conn);
            logger.debug("Closed connection: {}", conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            logger.trace("Received: {}", message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            logger.error("Websocket error", ex);
        }

        @Override
        public void onStart() {
            logger.info("Server started on: {}", getAddress());
        }

        public void broadcastMessage(String message) {
//            logger.info("WebSocket sending: {}",message);
            for (WebSocket conn : connections) {
                if (conn.isOpen()) {
                    conn.send(message);
                }
            }
        }
    }

    public WebSocketDataWriter(int port, List<String> filter) {
        server = new MinimalWebSocketServer(new InetSocketAddress(port));
        this.filter = filter;
//        server.start();
        Thread t = new Thread(server);
        t.start();
//        server.run();
        logger.info("WebSocket server started on port: {}", port);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeHeader(UUID uuid, List<String> headers) {
        Map<Integer, String> indexToHeader = DataListenerUtilities.indicesToHeaders(headers, filter == null || filter.isEmpty() ? null : filter);
        valueConverter.configure(indexToHeader);
    }

    @Override
    public void writeDataPoint(UUID uuid, double time, List<Value> dataPoint) {

        List<Object> values = new Vector<>();

        for (int i = 0; i < dataPoint.size(); i++) {
            Value d = dataPoint.get(i).deref();

            Object value = null;
            if (d.isNumericDecimal()) {
                if (floatFormatter == null) {
                    value = ((NumericValue) d).doubleValue();
                } else {
                    value = String.format(Locale.US, floatFormatter, ((NumericValue) d).doubleValue());
                }
            } else if (d.isNumeric()) {
                value = ((NumericValue) d).intValue();
            } else if (d instanceof BooleanValue) {
                value = ((BooleanValue) d).getValue();
            } else if (d instanceof StringValue) {
                value = ((StringValue) d).getValue();
            }
            values.add(value);
        }
        valueConverter.update(time, values);
        try {
            server.broadcastMessage(valueConverter.getJson());
        } catch (JsonProcessingException e) {
            logger.error("Couldn't serialize data", e);
        }
    }

    @Override
    public void close() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            logger.error("Failed to stop websocket server", e);
        }
    }

    public static void dumpWebsocketUi(Path path, int port) {
        try (InputStream res = WebSocketDataWriter.class.getResourceAsStream("graph.html")) {
            if (res != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(res, StandardCharsets.UTF_8));
                var bytes = IOUtils.toString(reader).replace("PORT", String.valueOf(port)).getBytes(StandardCharsets.UTF_8);
                Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
