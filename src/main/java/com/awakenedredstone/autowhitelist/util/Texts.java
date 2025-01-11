package com.awakenedredstone.autowhitelist.util;

import net.minecraft.text.Text;

public class Texts {
    public static Text playerPlaceholder(Text input, String player) {
        return DynamicPlaceholders.parseText(input, MapBuilder.single("player", Text.literal(player)));
    }

    public static Text playerPlaceholder(String input, String player) {
        return DynamicPlaceholders.parseText(input, MapBuilder.single("player", Text.literal(player)));
    }
}
