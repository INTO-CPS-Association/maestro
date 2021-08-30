package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TagNameGenerator {
    final Set<String> identifiers = new HashSet<>();
    //Should be taken from the MablLexer
    final Set<String> reserved = new HashSet<>(
            Arrays.asList("true", "false", "if", "while", "else", "out", "ref", "real", "uint", "bool", "int", "void", "string", "module", "expand",
                    "import", "simulation", "observable", "break"));

    public void addUsedIdentifier(String name) {
        this.identifiers.add(name);
    }

    public String getName() {
        return getName("tmp");
    }

    public String getName(String... prefixComponents) {
        if (prefixComponents == null || prefixComponents.length == 0) {
            return getName();
        }

        StringBuilder s = new StringBuilder(prefixComponents[0].toLowerCase());
        for (int i = 1; i < prefixComponents.length; i++) {
            String part = prefixComponents[i].toLowerCase();
            if (part.length() > 1) {
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
            }
            s.append(part);
        }
        return getName(s.toString());

    }

    public String getNameIgnoreCase(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            // TODO: Throw warning. Probably not meant to call this function.
            return this.getName();
        }

        if (!identifiers.contains(prefix) && !reserved.contains(prefix)) {
            identifiers.add(prefix);
            return prefix;
        }

        int postFix = 1;
        while (identifiers.contains(prefix + postFix) || reserved.contains(prefix + postFix)) {
            postFix++;
        }
        String name = prefix + postFix;
        identifiers.add(name);
        return name;
    }

    public String getName(String prefix) {

        if (prefix == null || prefix.isEmpty()) {
            // TODO: Throw warning. Probably not meant to call this function.
            return this.getName();
        }
        return getNameIgnoreCase(StringUtils.uncapitalize(prefix));
    }
}
