package com.awakenedredstone.autowhitelist.discord.commands.system;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.text.Text;

import java.util.Map;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;

public class HelpCommand {
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.help.failed"));

    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register((CommandManager.literal("help").executes((context) -> {
            Map<CommandNode<DiscordCommandSource>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), context.getSource());

            EmbedBuilder embedBuilder = new EmbedBuilder();
            DiscordCommandSource source = context.getSource();

            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle(Text.translatable("command.name.help").getString());
            embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());

            for (String string : map.values()) {
                embedBuilder.appendDescription(Text.literal("`" + AutoWhitelist.CONFIG.prefix() + string + "` | ").append(Text.translatable("command.description." + string.split(" ", 2)[0])).getString());
                embedBuilder.appendDescription("\n");
            }

            source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            return map.size();
            
        })).then(CommandManager.argument("command", StringArgumentType.greedyString()).executes((context) -> {
            ParseResults<DiscordCommandSource> parseResults = dispatcher.parse(StringArgumentType.getString(context, "command"), context.getSource());
            if (parseResults.getContext().getNodes().isEmpty()) {
                throw FAILED_EXCEPTION.create();
            } else {
                Map<CommandNode<DiscordCommandSource>, String> map = dispatcher.getSmartUsage(Iterables.getLast(parseResults.getContext().getNodes()).getNode(), context.getSource());

                EmbedBuilder embedBuilder = new EmbedBuilder();
                DiscordCommandSource source = context.getSource();

                embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
                embedBuilder.setTitle(Text.translatable("command.name.help").getString());
                embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());

                for (String string : map.values()) {
                    String result = parseResults.getReader().getString();
                    embedBuilder.appendDescription(Text.literal("`" + AutoWhitelist.CONFIG.prefix() + result + " " + string + "` | ").append(Text.translatable("command.description." + result)).getString());
                }
                if (map.isEmpty()) {
                    String result = parseResults.getReader().getString();
                    embedBuilder.appendDescription(Text.literal("`" + AutoWhitelist.CONFIG.prefix() + result + "` | ").append(Text.translatable("command.description." + result)).getString());
                }
                if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
//                    ((SlashCommandInteractionEvent)source.getEvent()).replyEmbeds(embedBuilder.build()).queue();
                } else {
                    source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                }

                return map.size();
            }
        })));
    }
}
