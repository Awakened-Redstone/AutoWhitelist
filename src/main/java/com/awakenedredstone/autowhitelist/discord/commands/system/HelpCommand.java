package com.awakenedredstone.autowhitelist.discord.commands.system;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.text.LiteralText;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.Map;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;

public class HelpCommand {
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.help.failed"));

    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
//        CommandDataImpl command = new CommandDataImpl("help", new TranslatableText("command.description.help").getString());
//        command.addOptions(new OptionData(OptionType.STRING, "command", new TranslatableText("command.description.help.command").getString()));
//        jda.upsertCommand(command).queue();

        dispatcher.register((CommandManager.literal("help").executes((context) -> {
            Map<CommandNode<DiscordCommandSource>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), context.getSource());

            EmbedBuilder embedBuilder = new EmbedBuilder();
            DiscordCommandSource source = context.getSource();

            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle(new TranslatableText("command.name.help").getString());
            embedBuilder.setFooter("Minecraft PhoenixSC Edition");

            for (String string : map.values()) {
                embedBuilder.appendDescription(new LiteralText("`" + AutoWhitelist.getConfigData().prefix + string + "` | ").append(new TranslatableText("command.description." + string.split(" ", 2)[0])).getString());
                embedBuilder.appendDescription("\n");
            }

            if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
                ((SlashCommandInteractionEvent)source.getEvent()).replyEmbeds(embedBuilder.build()).queue();
            } else {
                source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            }

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
                embedBuilder.setTitle(new TranslatableText("command.name.help").getString());
                embedBuilder.setFooter("Minecraft PhoenixSC Edition");

                for (String string : map.values()) {
                    String result = parseResults.getReader().getString();
                    embedBuilder.appendDescription(new LiteralText("`" + AutoWhitelist.getConfigData().prefix + result + " " + string + "` | ").append(new TranslatableText("command.description." + result)).getString());
                }
                if (map.isEmpty()) {
                    String result = parseResults.getReader().getString();
                    embedBuilder.appendDescription(new LiteralText("`" + AutoWhitelist.getConfigData().prefix + result + "` | ").append(new TranslatableText("command.description." + result)).getString());
                }
                if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
                    ((SlashCommandInteractionEvent)source.getEvent()).replyEmbeds(embedBuilder.build()).queue();
                } else {
                    source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                }

                return map.size();
            }
        })));
    }
}
