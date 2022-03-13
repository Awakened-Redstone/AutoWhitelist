package com.awakenedredstone.autowhitelist.discord.api.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.text.LiteralText;
import com.awakenedredstone.autowhitelist.discord.api.text.MutableText;
import com.awakenedredstone.autowhitelist.discord.api.text.Text;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.awakenedredstone.autowhitelist.discord.api.util.Formatting;
import com.awakenedredstone.autowhitelist.discord.api.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;

public class CommandManager {
    private static final Logger LOGGER = LogManager.getLogger();
    public final CommandDispatcher<DiscordCommandSource> dispatcher = new CommandDispatcher<>();

    public CommandManager() {
        this.dispatcher.findAmbiguities((parent, child, sibling, inputs) -> LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", this.dispatcher.getPath(child), this.dispatcher.getPath(sibling), inputs));
        this.dispatcher.setConsumer((context, success, result) -> context.getSource().onCommandComplete(context, success, result));
    }

    public int execute(DiscordCommandSource commandSource, String command) {
        StringReader stringReader = new StringReader(command);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        commandSource.getApi().getProfiler().push(command);

        try {
            byte var18;
            try {
                return this.dispatcher.execute(stringReader, commandSource);
            } catch (CommandException var13) {
                commandSource.sendError(var13.getTextMessage());
                var18 = 0;
                return var18;
            } catch (CommandSyntaxException var14) {
                if (var14.getInput() != null && var14.getCursor() >= 0) {
                    int i = Math.min(var14.getInput().length(), var14.getCursor());
                    MutableText mutableText = Util.toText(var14.getRawMessage()).shallowCopy();
                    mutableText.append("\n");
                    if (i > 10) {
                        mutableText.append("...");
                    }

                    mutableText.append(var14.getInput().substring(Math.max(0, i - 10), i).replaceFirst("^/", AutoWhitelist.getConfigData().prefix));
                    if (i < var14.getInput().length()) {
                        Text text = (new LiteralText(var14.getInput().substring(i))).formatted(Formatting.RED, Formatting.UNDERLINE);
                        mutableText.append(text);
                    }

                    mutableText.append((new TranslatableText("command.context.here")).formatted(Formatting.RED, Formatting.ITALIC));
                    if (commandSource.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
                        embedBuilder.setTitle(" ");
                        embedBuilder.setDescription(mutableText.getString());
                        embedBuilder.setFooter(new TranslatableText("command.feedback.message.signature").getString());
                        embedBuilder.setColor(BotHelper.MessageType.ERROR.hexColor);
//                        ((SlashCommandInteractionEvent)commandSource.getEvent()).deferReply(true).queue(m -> m.editOriginal(new MessageBuilder().setEmbeds(embedBuilder.build()).build()).queue());
                    } else {
                        commandSource.sendError(mutableText);
                    }
                } else {
                    commandSource.sendError(Util.toText(var14.getRawMessage()));
                }
            } catch (Exception var15) {
                MutableText mutableText2 = new LiteralText(var15.getMessage() == null ? var15.getClass().getName() : var15.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("Command exception: {}", command, var15);
                    StackTraceElement[] stackTraceElements = var15.getStackTrace();

                    for (int j = 0; j < Math.min(stackTraceElements.length, 3); ++j) {
                        mutableText2.append("\n\n").append(stackTraceElements[j].getMethodName()).append("\n ").append(stackTraceElements[j].getFileName()).append(":").append(String.valueOf(stackTraceElements[j].getLineNumber()));
                    }
                }

                commandSource.sendError((new TranslatableText("command.failed")));
                if (AutoWhitelist.getConfigData().devVersion) {
                    commandSource.sendError(new LiteralText(Util.getInnermostMessage(var15)));
                    LOGGER.error("'{}' threw an exception", command, var15);
                }

                return (byte) 0;
            }

            var18 = 0;
            return var18;
        } finally {
            commandSource.getApi().getProfiler().pop();
        }
    }

    public static LiteralArgumentBuilder<DiscordCommandSource> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<DiscordCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static Predicate<String> getCommandValidator(CommandParser parser) {
        return (string) -> {
            try {
                parser.parse(new StringReader(string));
                return true;
            } catch (CommandSyntaxException var3) {
                return false;
            }
        };
    }

    @Nullable
    public static <S> CommandSyntaxException getException(ParseResults<S> parse) {
        if (!parse.getReader().canRead()) {
            return null;
        } else if (parse.getExceptions().size() == 1) {
            return (CommandSyntaxException) parse.getExceptions().values().iterator().next();
        } else {
            return parse.getContext().getRange().isEmpty() ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader()) : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
        }
    }

    @FunctionalInterface
    public interface CommandParser {
        void parse(StringReader reader) throws CommandSyntaxException;
    }
}
