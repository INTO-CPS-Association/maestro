package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.EnvironmentException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class LegacyMMSupport {
    public static Map<String, String> adjustFmi2SimulationEnvironmentConfiguration(
            Fmi2SimulationEnvironmentConfiguration config) throws EnvironmentException {
        // Calculate the instance remapping
        Map<String, String> instanceRemapping = calculateInstanceRemapping(config.connections);

        // Apply it to connections, variablesToLog, logVariables and livestream.
        fixVariableToVariablesMap(instanceRemapping, config.connections);
        fixKeyInstanceToVariablesMap(instanceRemapping, config.variablesToLog);
        fixKeyInstanceToVariablesMap(instanceRemapping, config.logVariables);
        fixKeyInstanceToVariablesMap(instanceRemapping, config.livestream);
        return instanceRemapping;
    }

    public static <T> void fixKeyInstanceToVariablesMap(Map<String, String> remappings, Map<String, T> variables) throws EnvironmentException {
        List<String> objectsToRemove = new ArrayList<>();
        Map<String, T> newMap = new HashMap<>();
        if (variables != null) {
            for (Map.Entry<String, T> entry : variables.entrySet()) {
                KeyInstance entry_ = KeyInstance.ofKeyInstance(entry.getKey());
                if (remappings.containsKey(entry_.instance)) {
                    objectsToRemove.remove(entry.getKey());
                    entry_.instance = remappings.get(entry_.instance);
                    newMap.put(entry_.toKeyInstance(), entry.getValue());
                }
            }
            objectsToRemove.forEach(x -> variables.remove(x));
            variables.putAll(newMap);
        }
    }

    public static <T> void fixVariableToXMap(Map<String, String> remappings, Map<String, T> variables) throws EnvironmentException {
        List<String> objectsToRemove = new ArrayList<>();
        Map<String, T> newMap = new HashMap<>();
        if (variables != null) {
            for (Map.Entry<String, T> entry : variables.entrySet()) {
                KeyInstance entry_ = KeyInstance.ofVariable(entry.getKey());
                if (remappings.containsKey(entry_.instance)) {
                    objectsToRemove.add(entry.getKey());
                    entry_.instance = remappings.get(entry_.instance);
                    newMap.put(entry_.toVariable(), entry.getValue());
                }
            }
            objectsToRemove.forEach(x -> variables.remove(x));
            variables.putAll(newMap);
        }


    }

    public static void fixVariableToVariablesMap(Map<String, String> remappings, Map<String, List<String>> map) throws EnvironmentException {
        Map<String, List<String>> newMap = new HashMap<>();
        List<String> objectsToRemove = new ArrayList<>();
        if (map != null && map.size() > 0 && remappings != null && remappings.size() > 0) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {

                List<String> newValues = new ArrayList<>();
                boolean replaceValues = false;
                for (String value : entry.getValue()) {
                    KeyInstance value_ = KeyInstance.ofVariable(value);
                    if (remappings.containsKey(value_.instance)) {
                        value_.instance = remappings.get(value_.instance);
                        newValues.add(value_.toVariable());
                        replaceValues = true;
                    } else {
                        newValues.add(value);
                    }
                }

                KeyInstance entry_ = KeyInstance.ofVariable(entry.getKey());
                // If the key has to be changed, then change it and create a new key
                if (remappings.containsKey(entry_.instance)) {
                    entry_.instance = remappings.get(entry_.instance);
                    newMap.put(entry_.toVariable(), newValues);
                    objectsToRemove.add(entry.getKey());
                } else {
                    // Otherwise, if the key does not have to be changed, then only add an entry, if the list has to be changed.
                    if (replaceValues) {
                        newMap.put(entry.getKey(), newValues);
                    }
                }
            }
        }
        objectsToRemove.forEach(x -> map.remove(x));
        map.putAll(newMap);
    }

    /**
     * In a multi-model FMU keys and instances are not allowed to have the same name.
     * This function finds all cases where this is the case and then renames the instances.
     *
     * @param connections
     * @return
     * @throws EnvironmentException
     */
    public static Map<String, String> calculateInstanceRemapping(Map<String, List<String>> connections) throws EnvironmentException {
        HashMap<String, String> instanceRemapping = new HashMap<>();
        // Get all key instances pairs
        Set<KeyInstance> uniqueStrings = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : connections.entrySet()) {
            uniqueStrings.add(KeyInstance.ofVariable(entry.getKey()));
            for (String entry_ : entry.getValue()) {
                uniqueStrings.add(KeyInstance.ofVariable(entry_));
            }
        }

        // Function that detects whether the 1st argument matches any keys or instances in the 2nd argument
        BiFunction<String, Stream<KeyInstance>, Boolean> instanceNameIsInvalid =
                (instanceName, keyInstances) -> keyInstances.anyMatch(x -> x.key.equals(instanceName) || x.instance.equals(instanceName));
        // Extract all the KeyInstance cases where the key and instance are the same.
        List<KeyInstance> sameKeyInstance = uniqueStrings.stream().filter(x -> x.instance.equals(x.key)).collect(Collectors.toList());

        // Find a correct instancename and fix the instanceRemapping.
        for (KeyInstance keyInstanceEntry : sameKeyInstance) {
            int adder = 1;
            String newInstanceName = null;
            do {
                newInstanceName = keyInstanceEntry.instance + "_" + adder;
                adder++;
            } while (instanceNameIsInvalid.apply(newInstanceName, uniqueStrings.stream().filter(x -> x != keyInstanceEntry)));
            instanceRemapping.put(keyInstanceEntry.instance, newInstanceName);
        }
        return instanceRemapping;
    }

    public static class LegacyEnvConfigResult {
        public final Map<String, String> remapping;
        public final Fmi2SimulationEnvironmentConfiguration adjustedConfiguration;

        public LegacyEnvConfigResult(Map<String, String> remapping, Fmi2SimulationEnvironmentConfiguration adjustedConfiguration) {
            this.remapping = remapping;
            this.adjustedConfiguration = adjustedConfiguration;
        }
    }
}
