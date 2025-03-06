package org.intocps.maestro;

import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.interpreter.extensions.MEnvLifecycleHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class MablRuntimeDataGenerator {
    final ISimulationEnvironment env;
    private final Mabl.RuntimeData runtimeData;

    public MablRuntimeDataGenerator(ISimulationEnvironment env, Mabl.RuntimeData runtimeData) {
        this.env = env;
        this.runtimeData = runtimeData;
    }

    public Object getRuntimeData() {
        Map<String, Object> data = new HashMap<>();

        var writers = new Vector<>();

        if (this.runtimeData != null) {


            if (this.runtimeData.getRuntimeEnvironmentVariables() != null) {
                data.put(MEnvLifecycleHandler.ENVIRONMENT_VARIABLES, this.runtimeData.getRuntimeEnvironmentVariables());
            }

            if (this.runtimeData.getLivestreamVariables() != null && !this.runtimeData.getLivestreamVariables().isEmpty()) {


                if (runtimeData.getWebsocketPort() != null) {
                    writers.add(new HashMap<>() {
                        {
                            put("type", "WebSocket");
                            put("port", runtimeData.getWebsocketPort());
                            if (runtimeData.getLiveStreamInterval() != null) {
                                put("interval", runtimeData.getLiveStreamInterval());
                            }
                            if (runtimeData.getLivestreamVariables() != null) {
                                put("filter",
                                        runtimeData.getLivestreamVariables().entrySet().stream()
                                                .flatMap(m -> m.getValue().stream().map(v -> m.getKey() + "." + v))
                                                .collect(
                                                        Collectors.toList()));
                            }
                        }
                    });
                } else {
                    writers.add(new HashMap<>() {
                        {
                            put("type", "CSV_LiveStream");
                            put("filename", "outputs.csv");
                            if (runtimeData.getLiveStreamInterval() != null) {
                                put("interval", runtimeData.getLiveStreamInterval());
                            }
                            if (runtimeData.getLivestreamVariables() != null) {
                                put("filter",
                                        runtimeData.getLivestreamVariables().entrySet().stream()
                                                .flatMap(m -> m.getValue().stream().map(v -> m.getKey() + "." + v))
                                                .collect(
                                                        Collectors.toList()));
                            }
                        }
                    });
                }
            }
        }

        writers.add(new HashMap<>() {
            {
                put("type", "CSV");
                put("filename", "outputs.csv");
                if (runtimeData.getOutputVariables() != null) {
                    put("filter",
                            runtimeData.getOutputVariables().entrySet().stream().flatMap(m -> m.getValue().stream().map(v -> m.getKey() + "." + v)).collect(
                                    Collectors.toList()));
                }
            }
        });

        if (!writers.isEmpty()) {
            data.put("DataWriter", writers);
        }


        return data;
    }
}
