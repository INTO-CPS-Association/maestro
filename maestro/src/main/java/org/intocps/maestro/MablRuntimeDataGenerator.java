package org.intocps.maestro;

import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MablRuntimeDataGenerator {
    final ISimulationEnvironment env;
    private final Map<String, Object> runtimeEnvironmentVariables;

    public MablRuntimeDataGenerator(ISimulationEnvironment env, Map<String, Object> runtimeEnvironmentVariables) {
        this.env = env;
        this.runtimeEnvironmentVariables = runtimeEnvironmentVariables;
    }

    public Object getRuntimeData() {
        Map<String, Object> data = new HashMap<>();

        data.put("DataWriter", Arrays.asList(new HashMap<>() {
            {
                put("type", "CSV");
                put("filename", "outputs.csv");
            }
        }));

        if (this.runtimeEnvironmentVariables != null) {
            data.put(DefaultExternalValueFactory.MEnvLifecycleHandler.ENVIRONMENT_VARIABLES, this.runtimeEnvironmentVariables);
        }

        return data;
    }
}
