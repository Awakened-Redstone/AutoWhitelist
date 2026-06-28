package com.awakenedredstone.autowhitelist.util.string;

import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import eu.pb4.placeholders.impl.textparser.SingleTagLikeParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Texts {
    private static final TagLikeParser.Provider COMMON_PLACEHOLDERS = TagLikeParser.Provider.placeholder(ServerPlaceholderContext.COMMON_KEY, Placeholders.COMMON_PLACEHOLDER_GETTER);
    private static final TagLikeParser.Provider SERVER_PLACEHOLDERS = TagLikeParser.Provider.placeholder(ServerPlaceholderContext.SERVER_KEY, Placeholders.SERVER_PLACEHOLDER_GETTER);
    private static final NodeParser PLACEHOLDER_PARSER = NodeParser.builder()
      .add(new SingleTagLikeParser(TagLikeParser.PLACEHOLDER_ALTERNATIVE, COMMON_PLACEHOLDERS))
      .add(new SingleTagLikeParser(TagLikeParser.PLACEHOLDER_ALTERNATIVE, SERVER_PLACEHOLDERS))
      .serverPlaceholders()
      .build();

    public static Component placeholder(String input) {
        return PLACEHOLDER_PARSER.parseComponent(input, ServerPlaceholderContext.of(ServerDetails.getServer()).asParserContext());
    }

    public static Component playerPlaceholder(String input, PlayerProfile player) {
        return PLACEHOLDER_PARSER.parseComponent(input, ServerPlaceholderContext.of(player.asGameProfile(), ServerDetails.getServer()).asParserContext());
    }

    public static String translated(String translation, Object... args) {
        return Component.translatable(translation, args).getString();
    }

    public static MutableComponent translatedComponent(String translation, Object... args) {
        return Component.literal(translated(translation, args));
    }
}
