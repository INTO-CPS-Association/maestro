package org.intocps.maestro.interpreter.values.datawriter;

import java.util.ArrayList;
import java.util.List;

public class DataListenerUtilities {

    public static List<Integer> indicesOfInterest(List<String> headers, List<String> headersOfInterest) {
        List<Integer> indicesOfInterest = new ArrayList<>();
        // Discover the headers of interest and store the index of these
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (headersOfInterest.contains(header)) {
                indicesOfInterest.add(i);
            }
        }
        return indicesOfInterest;
    }
}
