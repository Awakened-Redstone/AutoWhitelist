package com.awakenedredstone.autowhitelist.util;

public class TimeParser {

    public static int parseTime(String timeString) {
        int time = 0;
        int multiplier = 1;
        StringBuilder number = new StringBuilder();
        for (char c : timeString.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                multiplier = switch (c) {
                    case 's' -> 1;
                    case 'm' -> 60;
                    case 'h' -> 3600;
                    case 'd' -> 86400;
                    case 'w' -> 604800;
                    case 'M' -> 2592000;
                    case 'y' -> 31536000;
                    default -> multiplier;
                };
            }
        }
        if (!number.isEmpty()) {
            int num = Integer.parseInt(number.toString()) * multiplier;
            // Prevent overflow
            time = (time + num < 0) ? Integer.MAX_VALUE : time + num;
        }
        return time;
    }
}
