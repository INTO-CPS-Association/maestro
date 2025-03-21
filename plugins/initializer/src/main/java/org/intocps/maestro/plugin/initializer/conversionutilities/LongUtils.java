package org.intocps.maestro.plugin.initializer.conversionutilities;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class LongUtils {

    public static final Collector<Long, ?, long[]> TO_LONG_ARRAY = Collectors.collectingAndThen(Collectors.toList(), LongUtils::listToArray);

    private LongUtils() {
    }

    public static long[] listToArray(List<Long> list) {
        int length = list.size();
        long[] arr = new long[length];
        for (int i = 0; i < length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
