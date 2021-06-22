package org.intocps.maestro.plugin.initializer.conversionutilities;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class BooleanUtils {

    public static final Collector<Boolean, ?, boolean[]> TO_BOOLEAN_ARRAY =
            Collectors.collectingAndThen(Collectors.toList(), BooleanUtils::listToArray);

    private BooleanUtils() {
    }

    public static boolean[] listToArray(List<Boolean> list) {
        int length = list.size();
        boolean[] arr = new boolean[length];
        for (int i = 0; i < length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}


