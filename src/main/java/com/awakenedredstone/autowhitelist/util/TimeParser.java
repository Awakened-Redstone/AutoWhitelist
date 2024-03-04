package com.awakenedredstone.autowhitelist.util;

public class TimeParser {
    public static int parseTime(String timeString) {
        int time = 0;
        StringBuilder number = new StringBuilder();
        for (char c : timeString.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                int multiplier = switch (c) {
                    case 's' -> 1;         // Second
                    case 'm' -> 60;        // Minute
                    case 'h' -> 3600;      // Hour
                    case 'd' -> 86400;     // Day
                    case 'w' -> 604800;    // Week
                    case 'M' -> 2592000;   // Month
                    case 'y' -> 31536000;  // Year
                    default -> 0;
                };
                if (multiplier == 0) continue;
                if (!number.isEmpty()) {
                    int num = Integer.parseInt(number.toString()) * multiplier;
                    // Prevent overflow
                    time = (time + num < 0) ? Integer.MAX_VALUE : time + num;
                    number.setLength(0);  // Reset for the next unit
                }
            }
        }
        // Handle the case where the time string ends with a number (default to seconds)
        if (!number.isEmpty()) {
            int num = Integer.parseInt(number.toString());
            time = (time + num < 0) ? Integer.MAX_VALUE : time + num;
        }
        return time;
    }
}
