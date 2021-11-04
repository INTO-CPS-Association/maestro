package org.intocps.maestro.template;

import org.apache.commons.lang3.RegExUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains facilities to convert invalid names from multi-model to mabl specification
 */
public class NameMapper {
    public static boolean startsWithLowerLetter(String string) {
        char c = string.charAt(0);
        return c >= 'a' && c <= 'z';
    }

    /**
     * This method removes invalid characters. Invalid characters are characters not allowed in a variable name.
     * Final Regex for variable names: ^[^a-zA-Z_]+|[^a-zA-Z_0-9]
     * First the name is filtered by regex ^[^a-zA-Z_0-9]+|[^a-zA-Z_0-9]
     * If the subsequent name begins with '0'-'0', then 'fmu' is prepended.
     *
     * @param string
     * @return
     */
    public static String handleInvalidCharacters(String string) {
        // Remove invalid characters.
        String regex1Result = RegExUtils.removeAll(string, "^[^a-zA-Z_0-9]+|[^a-zA-Z_0-9]");
        // It might be the case that the string now begins with a number, which is not allowed.
        // Prepend fmu in front.
        String digitResult = regex1Result;
        char c = digitResult.charAt(0);
        if (c >= '0' && c <= '9') {
            digitResult = "fmu" + digitResult;
        }

        return digitResult;
    }

    public static String makeSafeFMULexName(String fmuLexName, NameMapperState nameMapperState) {
        String newFmuLexName = handleInvalidCharacters(fmuLexName);
        while (nameMapperState.invalidNames.contains(newFmuLexName)) {
            nameMapperState.currentAppendCounter++;
            newFmuLexName = newFmuLexName + nameMapperState.currentAppendCounter;
        }
        nameMapperState.invalidNames.add(newFmuLexName);

        return newFmuLexName;
    }

    public String fixFmuName(String fmuName) {
        return null;
    }

    public static class NameMapperState {
        Set<String> invalidNames = new HashSet<>();
        int currentAppendCounter = 0;
    }
}


