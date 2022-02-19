package com.awakenedredstone.autowhitelist.discord.api.util;

import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.text.LiteralText;
import com.awakenedredstone.autowhitelist.discord.api.text.MutableText;
import com.awakenedredstone.autowhitelist.discord.api.text.ParsableText;
import com.awakenedredstone.autowhitelist.discord.api.text.Text;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

public class Util {
    public static LongSupplier nanoTimeSupplier = System::nanoTime;

    public static Text toText(Message message) {
        return message instanceof Text ? (Text)message : new LiteralText(message.getString());
    }

    public static MutableText parse(@Nullable DiscordCommandSource source, Text text, @Nullable User sender, int depth) throws CommandSyntaxException {
        if (depth > 100) {
            return text.shallowCopy();
        } else {
            MutableText mutableText = text instanceof ParsableText ? ((ParsableText)text).parse(source, sender, depth + 1) : text.copy();

            for (Text text2 : text.getSiblings()) {
                mutableText.append(parse(source, text2, sender, depth + 1));
            }

            return mutableText.fillStyle(text.getStyle());
        }
    }

    public static String getInnermostMessage(Throwable t) {
        if (t.getCause() != null) {
            return getInnermostMessage(t.getCause());
        } else {
            return t.getMessage() != null ? t.getMessage() : t.toString();
        }
    }

    public static long getMeasuringTimeNano() {
        return nanoTimeSupplier.getAsLong();
    }

}
