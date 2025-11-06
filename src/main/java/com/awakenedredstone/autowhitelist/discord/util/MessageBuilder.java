package com.awakenedredstone.autowhitelist.discord.util;

public class MessageBuilder {
    public static String formatDiscordTimestamp(long timestamp) {
        return "<t:" + (timestamp / 1000) + ":R>";
    }
}
