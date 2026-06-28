package com.awakenedredstone.autowhitelist.discord.message;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.network.chat.Component;
import org.reactivestreams.Publisher;

import java.awt.*;
import java.util.UUID;

public class MessageUtils {
    public static Component timestampMillis(long timestamp, String type) {
        return timestamp(timestamp / 1000, type);
    }

    public static Component timestamp(long timestamp, String type) {
        return Component.translatable("discord.autowhitelist.timestamp." + type, timestamp);
    }

    public static boolean ephemeral() {
        return AutoWhitelist.config().discord.ephemeralReplies;
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

        public int getHexColor() {
            return hexColor;
        }
    }

    public record HandlerData<T>(UUID id, Publisher<T> publisher) {}
}
