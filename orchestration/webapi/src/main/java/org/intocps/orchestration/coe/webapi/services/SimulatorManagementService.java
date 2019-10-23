package org.intocps.orchestration.coe.webapi.services;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.orchestration.coe.webapi.SimulationLauncher;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimulatorManagementService {
    static Map<String, Process> simulators = new HashMap<>();

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

    private File getSimulatorDirectory(String simulatorId) {
        return Paths.get("Simulators", simulatorId).toFile();
    }

    public boolean delete(String simulatorId) throws IOException {
        Process p = simulators.get(simulatorId);
        if (p != null) {
            p.destroy();
            simulators.remove(simulatorId);
            FileUtils.deleteDirectory(getSimulatorDirectory(simulatorId));
            return true;
        }
        return false;
    }

    public String create(String simulatorId) throws Exception {
        File base = getSimulatorDirectory(simulatorId);

        Pair<Integer, Process> integerProcessPair = launchNewClone(base);
        if (integerProcessPair == null || integerProcessPair.getValue() == null || !integerProcessPair.getValue().isAlive()) {
            throw new Exception("failed to launch simulator");
        }
        simulators.put(simulatorId, integerProcessPair.getValue());

        return getHostname() + ":" + integerProcessPair.getKey();
    }

    public void terminateApplication() {
        System.exit(0);
    }
}
