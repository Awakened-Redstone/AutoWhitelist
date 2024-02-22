package com.awakenedredstone.autowhitelist.discord.api.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
/*? if >=1.19 {*/import net.minecraft.screen.ScreenTexts;/*?} */
import net.minecraft.text.*;
/*? if >=1.19 {*/
/*?} else {*//*
*//*?} */
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandManager {
    private static final Logger LOGGER = LogManager.getLogger();
    public final CommandDispatcher<DiscordCommandSource> dispatcher = new CommandDispatcher<>();

    public CommandManager() {
        this.dispatcher.findAmbiguities((parent, child, sibling, inputs) -> LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", this.dispatcher.getPath(child), this.dispatcher.getPath(sibling), inputs));
        this.dispatcher.setConsumer((context, success, result) -> context.getSource().onCommandComplete(context, success, result));
    }

    public static LiteralArgumentBuilder<DiscordCommandSource> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<DiscordCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public int execute(DiscordCommandSource commandSource, String command) {
        StringReader stringReader = new StringReader(command);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        try {
            return this.dispatcher.execute(stringReader, commandSource);
        } catch (CommandSyntaxException exception) {
            int i;
            if (exception.getInput() != null && exception.getCursor() >= 0) {
                i = Math.min(exception.getInput().length(), exception.getCursor());
                MutableText mutableText = /*? if >=1.19 {*/Text.empty()/*?} else {*//*LiteralText.EMPTY.copy()*//*?}*/.formatted(net.minecraft.util.Formatting.GRAY);
                mutableText.append(Texts.toText(exception.getRawMessage()));
                mutableText.append("\n");
                if (i > 10) {
                    mutableText.append(/*? if >=1.19 {*/ScreenTexts.ELLIPSIS/*?} else {*//*"..."*//*?}*/);
                }
                mutableText.append(exception.getInput().substring(Math.max(0, i - 10), i).replaceFirst("^/", AutoWhitelist.CONFIG.prefix));
                if (i < exception.getInput().length()) {
                    MutableText text = /*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(exception.getInput().substring(i)).formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.UNDERLINE);
                    mutableText.append(text);
                }
                mutableText.append(/*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/("command.context.here").formatted(net.minecraft.util.Formatting.RED, Formatting.ITALIC));
                commandSource.sendError(mutableText);
            } else {
                commandSource.sendError(Texts.toText(exception.getRawMessage()));
            }
        } catch (Exception exception) {
            MutableText mutableText2 = /*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Command exception: {}{}", AutoWhitelist.CONFIG.prefix, command);
                StackTraceElement[] stackTraceElements = exception.getStackTrace();
                for (int j = 0; j < Math.min(stackTraceElements.length, 3); ++j) {
                    mutableText2.append("\n\n").append(stackTraceElements[j].getMethodName()).append("\n ").append(stackTraceElements[j].getFileName()).append(":").append(String.valueOf(stackTraceElements[j].getLineNumber()));
                }
            }
            MutableText mutableText = /*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/("command.failed").styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, mutableText2)));
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                mutableText.append("\n");
                mutableText.append(/*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/(Util.getInnermostMessage(exception)));
                LOGGER.error("'{}{}' threw an exception", AutoWhitelist.CONFIG.prefix, command);
            }
            commandSource.sendError(mutableText);
            return 0;
        }

        return 0;
    }
}
