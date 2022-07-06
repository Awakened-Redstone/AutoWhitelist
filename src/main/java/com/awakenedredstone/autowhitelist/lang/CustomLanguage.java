package com.awakenedredstone.autowhitelist.lang;

import com.google.common.collect.Maps;
import net.minecraft.client.resource.language.ReorderingUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Language;

import java.util.Map;

public class CustomLanguage extends Language {
    public static final Map<String, String> translations = Maps.newHashMap();
    private final boolean rightToLeft = false;
    private static final Language instance = new CustomLanguage();

    public String get(String key) {
        return translations.getOrDefault(key, key);
    }

    public boolean hasTranslation(String key) {
        return translations.containsKey(key);
    }

    public boolean isRightToLeft() {
        return this.rightToLeft;
    }

    public OrderedText reorder(StringVisitable text) {
        return ReorderingUtil.reorder(text, this.rightToLeft);
    }

    public static Language getInstance() {
        return instance;
    }
}
