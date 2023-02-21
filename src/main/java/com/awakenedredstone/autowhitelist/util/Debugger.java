package com.awakenedredstone.autowhitelist.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Deprecated(forRemoval = true)
public class Debugger {

    public static final Map<String, long[]> timings = new HashMap<>();

    @Deprecated(forRemoval = true)
    public static void analyzeTimings(String method, Dummy dummy) {
        long start = System.nanoTime();
        dummy.dummy();
        long time = ((System.nanoTime() - start) / 1000);
        timings.putIfAbsent(method, new long[]{time});
        List<Long> longs = Arrays.stream(timings.get(method)).boxed().collect(Collectors.toList());
        longs.add(time);
        timings.put(method, longs.stream().mapToLong(l -> l).toArray());
    }

    public static String formatTimings(long time) {
        String metric = "\u00B5s";
        if (time > 5000) {
            time /= 1000;
            metric = "ms";
        }
        if (time > 10000) {
            time /= 1000;
            metric = "s";
        }
        return time + metric;
    }

    public static String formatTimings(double time) {

        String metric = "\u00B5s";
        if (time > 5000) {
            time /= 1000;
            metric = "ms";
        }
        if (time > 10000) {
            time /= 1000;
            metric = "s";
        }
        return String.format("%.2f", time) + metric;
    }

    public interface Dummy {
        void dummy();
    }
}
