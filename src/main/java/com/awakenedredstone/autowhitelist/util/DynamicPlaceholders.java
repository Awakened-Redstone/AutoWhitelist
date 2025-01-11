package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import eu.pb4.placeholders.api.PlaceholderContext;
/*? if >=1.20.5 {*/
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
/*?} else {*/
/*import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
*//*?}*/
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.text.Text;

import java.util.Map;

public class DynamicPlaceholders {
    public static Text parseText(String inputText, Map<String, Text> placeholders) {
        return parseText(Text.literal(inputText), placeholders);
    }

    public static Text parseText(Text inputText, Map<String, Text> placeholders) {
        /*? if >=1.20.5 {*/
        TagLikeParser parser = TagLikeParser.placeholder(TagLikeParser.PLACEHOLDER_ALTERNATIVE, PlaceholderContext.KEY, id -> getPlaceholder(id, placeholders));
        return parser.parseText(TextNode.convert(inputText), PlaceholderContext.of(AutoWhitelist.getServer()).asParserContext());
        /*?} else {*/
        /*return Placeholders.parseText(inputText,
          PlaceholderContext.of(AutoWhitelist.getServer()),
          PatternPlaceholderParser.ALT_PLACEHOLDER_PATTERN_CUSTOM,
          id -> getPlaceholder(id, placeholders)
        );
        *//*?}*/
    }

    private static PlaceholderHandler getPlaceholder(String id, Map<String, Text> placeholders) {
        return placeholders.containsKey(id) ? (ctx, arg) -> PlaceholderResult.value(placeholders.get(id)) : Placeholders.DEFAULT_PLACEHOLDER_GETTER.getPlaceholder(id);
    }
}
