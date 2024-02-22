package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
/*? if >=1.19 {*/
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
/*?} else {*//*
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderHandler;
import eu.pb4.placeholders.PlaceholderResult;
*//*?} */

/*? if <1.19 {*//*
import net.minecraft.text.LiteralText;
*//*?}*/
import net.minecraft.text.Text;

import java.util.Map;

public class DynamicPlaceholders {

    public static Text parseText(String inputText, String player) {
        return parseText(/*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(inputText), player);
    }

    public static Text parseText(Text inputText, String player) {
        Map<String, Text> placeholders = Map.of("player", /*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(player));
        /*? if >=1.19 {*/
        return Placeholders.parseText(inputText, PlaceholderContext.of(AutoWhitelist.getServer()),
          PatternPlaceholderParser.PLACEHOLDER_PATTERN_CUSTOM,
            id -> getPlaceholder(id, placeholders));
        /*?} else {*//*
        return PlaceholderAPI.parseTextCustom(inputText, AutoWhitelist.getServer(), placeholders, PlaceholderAPI.PLACEHOLDER_PATTERN_CUSTOM);
        *//*?} */
    }

    private static PlaceholderHandler getPlaceholder(String id, Map<String, Text> placeholders) {
        return placeholders.containsKey(id) ? (ctx, arg) -> PlaceholderResult.value(placeholders.get(id)) : Placeholders.DEFAULT_PLACEHOLDER_GETTER.getPlaceholder(id);
    }
}
