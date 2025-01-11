package com.awakenedredstone.autowhitelist.util;

import java.util.regex.Pattern;

public class Validation {
    public static final Pattern REGEX = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    public static boolean isValidMinecraftUsername(String username) {
        return REGEX.matcher(username).matches();
    }
}
