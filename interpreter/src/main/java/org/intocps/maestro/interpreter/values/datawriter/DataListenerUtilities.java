package org.intocps.maestro.interpreter.values.datawriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataListenerUtilities {

    public static Map<Integer, String> indicesToHeaders(List<String> headers, List<String> headersOfInterest) {
        Map<Integer, String> indicesToHeaders = new HashMap<>();
        // Discover the headers of interest and store the index of these
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (headersOfInterest.contains(header)) {
                indicesToHeaders.put(i, header);
            }
        }
        return indicesToHeaders;
    }
}
