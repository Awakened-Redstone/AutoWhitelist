package com.awakenedredstone.autowhitelist.util;

import discord4j.discordjson.possible.Possible;

public class Perhaps {
    public static <T> T orElse(Possible<T> possible, T other) {
        return possible.toOptional().orElse(other);
    }
}
