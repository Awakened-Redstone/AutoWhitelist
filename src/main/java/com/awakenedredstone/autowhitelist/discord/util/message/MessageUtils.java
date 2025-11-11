package com.awakenedredstone.autowhitelist.discord.util.message;

import net.minecraft.text.Text;

import java.awt.*;

public class MessageUtils {
    public static Text timestampMillis(long timestamp, String type) {
        return timestamp(timestamp / 1000, type);
    }

    public static Text timestamp(long timestamp, String type) {
        return Text.translatable("discord.autowhitelist.timestamp." + type, timestamp);
    }

    public enum Pallet {
        DEBUG(new Color(19, 40, 138)),
        NORMAL(new Color(0, 0, 0)), // Discord ignores pure black in embeds
        INFO(new Color(176, 154, 15)),
        SUCCESS(new Color(50, 134, 25)),
        WARNING(new Color(208, 102, 21)),
        ERROR(new Color(141, 29, 29)),
        FATAL(new Color(212, 4, 4));

        private final int hexColor;

        Pallet(Color hexColor) {
            this.hexColor = hexColor.getRGB();
        }
    }
}
