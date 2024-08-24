package com.awakenedredstone.autowhitelist.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/*? if <1.19 {*/
/*import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
*//*?}*/
/*? if >=1.20.5 {*/
import com.mojang.serialization.MapCodec;
 /*?} else {*/
/*import com.mojang.serialization.Codec;
*//*?}*/
/*? if >=1.18.2 {*/
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*?} else {*/
/*import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
*//*?}*/

import java.util.function.Function;
/*? if >=1.20 {*/
import java.util.function.Supplier;
/*?}*/

/**
 * Utility class to move common components that have stonecutter comments to make the code cleaner
 */
public class Stonecutter {
    public static <O> /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<O> entryCodec(final Function<RecordCodecBuilder.Instance<O>, ? extends App<RecordCodecBuilder.Mu<O>, O>> builder) {
        return RecordCodecBuilder./*? if <1.20.5 {*//*create*//*?} else {*/mapCodec/*?}*/(builder);
    }

    public static Logger logger(String logger) {
        return /*? if >=1.18.2 {*/LoggerFactory/*?} else {*//*LogManager*//*?}*/.getLogger(logger);
    }

    public static Logger logger(Class<?> logger) {
        return /*? if >=1.18.2 {*/LoggerFactory/*?} else {*//*LogManager*//*?}*/.getLogger(logger);
    }

    public static Text translatableText(String key) {
        return /*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/(key);
    }

    public static String translatedText(String key) {
        return /*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/(key).getString();
    }

    public static MutableText emptyText() {
        return /*? if >=1.19 {*/Text.empty()/*?} else {*//*(MutableText) new LiteralText("")*//*?}*/;
    }

    public static Identifier identifier(String namespace, String path) {
        return /*? if <1.19 {*//*new*//*?}*/ Identifier/*? if >=1.19 {*/.of/*?}*/(namespace, path);
    }

    public static <R> R getOrThrowDataResult(DataResult<R> dataResult) {
        return dataResult.getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/);
    }

    public static Text translatableText(String key, Object... args) {
        return /*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/(key, args);
    }

    public static Text literalText(String text) {
        return /*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(text);
    }

    public static /*? if >=1.20 {*/Supplier<Text>/*?} else {*//*Text*//*?}*/ feedbackText(Text text) {
        return /*? if >=1.20 {*/() ->/*?}*/ text;
    }
}
