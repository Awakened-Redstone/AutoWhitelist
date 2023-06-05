package com.awakenedredstone.autowhitelist.discord.api.util;

import com.awakenedredstone.autowhitelist.util.TextUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Optional;

public class Markdown {

    public static String formatText(Text text) {
        StringBuilder formatted = new StringBuilder();
        TextUtil.placeholder(text).visit((style, message) -> {
            Str toAdd = new Str(message);
            if (style.isBold()) appendStartEnd("**", toAdd);
            if (style.isItalic()) appendStartEnd("_", toAdd);
            if (style.isUnderlined()) appendStartEnd("__", toAdd);
            if (style.isStrikethrough()) appendStartEnd("~~", toAdd);
            formatted.append(toAdd.text);
            return Optional.empty();
        }, Style.EMPTY);
        return formatted.toString();
    }

    private static void appendStartEnd(String append, Str text) {
        text.text = append + text.text + append;
    }

    private static class Str {
        String text;

        Str(String text) {
            this.text = text;
        }
    }
}
