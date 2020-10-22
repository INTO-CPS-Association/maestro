package org.intocps.maestro;

import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MablRuntimeDataGenerator {
    final ISimulationEnvironment env;

    public MablRuntimeDataGenerator(ISimulationEnvironment env) {
        this.env = env;
    }

    public Object getRuntimeData() {
        Map<String, List<Map<String, Object>>> data = new HashMap<>();

        data.put("DataWriter", Arrays.asList(new HashMap<>() {
            {
                put("type", "CSV");
                put("filename", "output.csv");
            }
        }));

        return data;
    }
}
