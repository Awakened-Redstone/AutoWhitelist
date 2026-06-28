package com.awakenedredstone.moondust.jankson.util;

public interface Numbers {
    static boolean isByte(long number) {
        return Byte.MIN_VALUE <= number && Byte.MAX_VALUE >= number;
    }

    static boolean isShort(long number) {
        return Short.MIN_VALUE <= number && Short.MAX_VALUE >= number;
    }

    static boolean isInteger(long number) {
        return Integer.MIN_VALUE <= number && Integer.MAX_VALUE >= number;
    }
}
