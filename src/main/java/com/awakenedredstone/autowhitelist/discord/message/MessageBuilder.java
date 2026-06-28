package com.awakenedredstone.autowhitelist.discord.message;

import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.core.object.component.TextDisplay;

public interface MessageBuilder {
    static TextDisplay translated(String key, Object... args) {
        return TextDisplay.of(Texts.translated(key, args));
    }
}
