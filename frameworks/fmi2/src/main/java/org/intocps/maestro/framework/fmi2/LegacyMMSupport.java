package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.EnvironmentException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class LegacyMMSupport {
    private static final Map<String, String> originalNamesToNewNamesForInstances = new HashMap<>();
    private static final String MM_DELIMITER_REGEX = "\\.";

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
            objectsToRemove.forEach(variables::remove);
            variables.putAll(newMap);
        }
    }


    public static String revertInstanceName(String instanceName) {
        for (Map.Entry<String, String> instanceNameMapEntry : originalNamesToNewNamesForInstances.entrySet()) {
            if (instanceName.contains(instanceNameMapEntry.getValue())) {
                // Replace transformed instance name with original instance name
                return instanceName.replace(instanceNameMapEntry.getValue(), instanceNameMapEntry.getKey());
            }
        }
        return instanceName;
    }

    public static void fixLeadingNumeralsInInstanceNames(Fmi2SimulationEnvironmentConfiguration config) {
        transformLeadingNumeralsInInstanceToWord(config.logVariables);
        transformLeadingNumeralsInInstanceToWord(config.livestream);
        transformLeadingNumeralsInInstanceToWord(config.connections);
        transformLeadingNumeralsInInstanceToWord(config.variablesToLog);
    }

    private static void transformLeadingNumeralsInInstanceToWord(Map<String, List<String>> mapping) {
        if (mapping == null || mapping.entrySet().isEmpty()) {
            return;
        }
        // Stream, flatmap, combine and filter all strings in mapping to create new mapping of original instance name to new instance name
        Map<String, String> originalNamesToNewNames = Stream.concat(mapping.keySet().stream(), mapping.values().stream().flatMap(List::stream))
                .filter(k -> k.split(MM_DELIMITER_REGEX).length > 1).map(k -> k.split(MM_DELIMITER_REGEX)[1])
                .collect(Collectors.toMap((s) -> s, LegacyMMSupport::replaceDigitWithWord, (s1, s2) -> s1));

        // Replace values in mapping entry set
        Map<String, List<String>> intermediateMap = new HashMap<>(mapping);
        for (Map.Entry<String, List<String>> entry : intermediateMap.entrySet()) {
            String key = entry.getKey();

            // Transform key instance
            String[] keyElements = entry.getKey().split(MM_DELIMITER_REGEX);
            // First element in array is the fmu name, second is the instance name
            if (keyElements.length > 1) {
                mapping.remove(entry.getKey());

                for (Map.Entry<String, String> instanceNameMapEntry : originalNamesToNewNames.entrySet()) {
                    if (key.contains(instanceNameMapEntry.getKey())) {
                        key = key.replace(instanceNameMapEntry.getKey(), instanceNameMapEntry.getValue());
                        break;
                    }
                }
            }

            // Transform value instances
            List<String> value = new ArrayList<>(entry.getValue()).stream().map(s -> {
                for (Map.Entry<String, String> instanceNameMapEntry : originalNamesToNewNames.entrySet()) {
                    if (s.contains(instanceNameMapEntry.getKey())) {
                        return s.replace(instanceNameMapEntry.getKey(), instanceNameMapEntry.getValue());
                    }
                }
                return s;
            }).collect(Collectors.toList());

            // Replace value(s) in existing mapping
            mapping.put(key, value);
        }

        // Add mapping from original instance name to new instance name
        originalNamesToNewNamesForInstances.putAll(originalNamesToNewNames);
    }

    private static String replaceDigitWithWord(String replaceDigitWithWord) {
        String[] wordDigits = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

        String[] stringSplit = replaceDigitWithWord.split("");
        for (int i = 0; i < stringSplit.length; i++) {
            for (int j = 0; j < digits.length; j++) {
                if (stringSplit[i].equalsIgnoreCase(digits[j])) {
                    if (i + 1 < stringSplit.length) {
                        replaceDigitWithWord = replaceDigitWithWord.substring(0, i + 1) + stringSplit[i + 1].toUpperCase() +
                                replaceDigitWithWord.substring(i + 2, stringSplit.length);
                    }
                    replaceDigitWithWord = replaceDigitWithWord.replaceFirst(stringSplit[i], wordDigits[j]);
                    return replaceDigitWithWord;
                }
            }
        }
        return replaceDigitWithWord;
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
