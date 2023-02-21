package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.text.Text;

public class TextUtil {
    public static Text placeholder(Text text) {
        return Placeholders.parseText(text, PlaceholderContext.of(AutoWhitelist.server));
    }
}
