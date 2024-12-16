package com.awakenedredstone.autowhitelist.util;

public class NullUtils {
    public static <T> T orElse(T val, T defaultVal) {
        return val != null ? val : defaultVal;
    }
}
