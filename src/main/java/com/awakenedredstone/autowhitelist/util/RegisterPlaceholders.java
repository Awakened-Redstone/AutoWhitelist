package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.Map;

public class RegisterPlaceholders {

    public static Text parseText(String inputText, String player) {
        return parseText(Text.literal(inputText), player);
    }
    public static Text parseText(Text inputText, String player) {
        Map<String, Text> placeholders = Map.of("player", Text.literal(player));
        return Placeholders.parseText(inputText, PlaceholderContext.of(AutoWhitelist.server),
                Placeholders.PLACEHOLDER_PATTERN_CUSTOM,
                id -> getPlaceholder(id, placeholders));
    }

    private static PlaceholderHandler getPlaceholder(String id, Map<String, Text> placeholders) {
        return placeholders.containsKey(id) ? (ctx, arg) -> PlaceholderResult.value(placeholders.get(id)) : Placeholders.getPlaceholders().get(id);
    }
}