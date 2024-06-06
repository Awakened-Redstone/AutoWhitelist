package com.awakenedredstone.autowhitelist.util;

/*? if >=1.19 {*/
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
/*?} else {*/
/*import eu.pb4.placeholders.PlaceholderAPI;
*//*?}*/
import net.minecraft.text.Text;

import java.util.Map;

public class DynamicPlaceholders {

    public static Text parseText(String inputText, Map<String, Text> placeholders) {
        return parseText(Stonecutter.literalText(inputText), placeholders);
    }

    public static Text parseText(Text inputText, Map<String, Text> placeholders) {
        /*? if >=1.19 {*/
        return Placeholders.parseText(inputText,
          PlaceholderContext.of(AutoWhitelist.getServer()),
          PatternPlaceholderParser.ALT_PLACEHOLDER_PATTERN_CUSTOM,
          id -> getPlaceholder(id, placeholders)
        );
        /*?} else {*/
        /*return PlaceholderAPI.parsePredefinedText(inputText, PlaceholderAPI.PLACEHOLDER_PATTERN_CUSTOM, placeholders);
        *//*?}*/
    }

    /*? if >=1.19 {*/
    private static PlaceholderHandler getPlaceholder(String id, Map<String, Text> placeholders) {
        return placeholders.containsKey(id) ? (ctx, arg) -> PlaceholderResult.value(placeholders.get(id)) : Placeholders.DEFAULT_PLACEHOLDER_GETTER.getPlaceholder(id);
    }
    /*?}*/
}
