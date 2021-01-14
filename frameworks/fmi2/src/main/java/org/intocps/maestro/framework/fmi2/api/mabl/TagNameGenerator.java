package org.intocps.maestro.framework.fmi2.api.mabl;

import java.util.HashSet;
import java.util.Set;

public class TagNameGenerator {

    // class variable
    final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
    final java.util.Random rand = new java.util.Random();
    // consider using a Map<String,Boolean> to say whether the identifier is being used or not
    final Set<String> identifiers = new HashSet<>();

    public String getName() {
        return getName("tmp");
    }

    public String getName(String prefix) {
        if (!identifiers.contains(prefix)) {
            identifiers.add(prefix);
            return prefix;
        }

        int postFix = 0;
        while (identifiers.contains(prefix + postFix)) {
            postFix++;
        }
        String name = prefix + postFix;
        identifiers.add(name);
        return name;

/*        StringBuilder builder = new StringBuilder(prefix != null ? prefix : "");
        while (builder.toString().length() == 0) {
            int length = rand.nextInt(5) + 5;
            for (int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            if (identifiers.contains(builder.toString())) {
                builder = new StringBuilder(prefix != null ? prefix : "");
            }
        }

        String name = builder.toString();

        identifiers.add(name);
        return name;
        */

    }


}
