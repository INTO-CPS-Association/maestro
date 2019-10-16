package org.intocps.orchestration.coe.webapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FileUtils;
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
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orchestrator")
public class SimulatorManagementController {
    private final static Logger logger = LoggerFactory.getLogger(SimulatorManagementController.class);

    static Map<String, Process> simulators = new HashMap<>();

    @RequestMapping(value = "/ping")
    public String ping() {
        return "OK";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public CreateResponse create() throws Exception {

        String simulatorId = UUID.randomUUID().toString();
        logger.debug("Creating simulator: {}", simulatorId);

        File base = getSimulatorDirectory(simulatorId);

        Pair<Integer, Process> integerProcessPair = launchNewClone(base);
        if (integerProcessPair == null || integerProcessPair.getValue() == null || !integerProcessPair.getValue().isAlive()) {
            throw new Exception("failed to launch simulator");
        }
        simulators.put(simulatorId, integerProcessPair.getValue());
        return new CreateResponse(simulatorId, getHostname() + ":" + integerProcessPair.getKey());
    }

    private File getSimulatorDirectory(String simulatorId) {
        return Paths.get("Simulators", simulatorId).toFile();
    }

    @RequestMapping(value = "/{simulatorId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String simulatorId) throws Exception {
        logger.debug("Deleting simulator: {}", simulatorId);

        Process p = simulators.get(simulatorId);
        if (p != null) {
            p.destroy();
            simulators.remove(simulatorId);
            FileUtils.deleteDirectory(getSimulatorDirectory(simulatorId));
            return;
        }
        throw new Exception("Unknown simulator id: " + simulatorId);
    }

    @RequestMapping(value = "/terminate", method = RequestMethod.POST)
    public void terminate() throws Exception {
        logger.info("System terminating. NOW");
        System.exit(0);
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

    public Pair<Integer, Process> launchNewClone(File base) throws IOException {
        int port = getFreeport();
        return Pair.of(port, SimulationLauncher.restartApplication(base, "-Dserver.port=" + port));
    }

    public class CreateResponse {
        @JsonProperty("instance_id")
        public final String instanceId;
        @JsonProperty("instance_url")
        public final String instanceUrl;

        public CreateResponse(String instanceId, String instanceUrl) {
            this.instanceId = instanceId;
            this.instanceUrl = instanceUrl;
        }

    }
}