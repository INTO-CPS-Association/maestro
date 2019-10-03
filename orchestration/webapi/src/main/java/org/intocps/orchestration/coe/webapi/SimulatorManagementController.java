package org.intocps.orchestration.coe.webapi;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorManagementController {
    private final static Logger logger = LoggerFactory.getLogger(SimulatorManagementController.class);
    static int simulatorId = 0;


    static Map<Integer, Process> simulators = new HashMap<>();

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public CreateResponse create() throws Exception {

        int id = simulatorId++;
        logger.debug("Creating simulator: {}", id);
        Pair<Integer, Process> integerProcessPair = launchNewClone();
        if (integerProcessPair == null || integerProcessPair.getValue() == null || !integerProcessPair.getValue().isAlive()) {
            throw new Exception("failed to launch simulator");
        }
        simulators.put(id, integerProcessPair.getValue());
        return new CreateResponse("" + id, getHostname() + ":" + integerProcessPair.getKey());
    }

    @RequestMapping(value = "/{simulatorId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String simulatorId) {
        logger.debug("Deleting simulator: {}", simulatorId);
    }

    private Integer getFreeport() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();

        }
    }

    public String getHostname() throws UnknownHostException {
        String hostname = "Unknown";

        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException ex) {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            return addr.toString();
        }
    }

    public Pair<Integer, Process> launchNewClone() throws IOException, URISyntaxException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(SimulationController.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        /* is it a jar file? */
        if (!currentJar.getName().endsWith(".jar")) {
            return null;
        }

        /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        int port = getFreeport();
        command.add("–server.port=" + port);

        final ProcessBuilder builder = new ProcessBuilder(command);
        return Pair.of(port, builder.start());
    }

    public class CreateResponse {
        public final String id;
        public final String url;

        public CreateResponse(String id, String url) {
            this.id = id;
            this.url = url;
        }
    }
}