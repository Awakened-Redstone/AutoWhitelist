package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
/*? if >=1.19 {*//*
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
*//*?} else {*/
import eu.pb4.placeholders.PlaceholderAPI;
/*?} */

import net.minecraft.text.Text;

public class TextUtil {
    public static Text placeholder(Text text) {
        return /*? if >=1.19 {*//*Placeholders*//*?} else {*/PlaceholderAPI/*?}*/.parseText(text, /*? if >=1.19 {*//*PlaceholderContext.of(*//*?}*/AutoWhitelist.getServer()/*? if >=1.19 {*//*)*//*?}*/);
    }
}
