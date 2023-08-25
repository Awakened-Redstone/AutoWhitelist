package com.awakenedredstone.autowhitelist.discord.commands.debug;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Objects;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class BotStatusCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("botstatus")
            .requires((source) -> AutoWhitelist.CONFIG.admins().stream().anyMatch(v -> Objects.equals(v, source.getUser().getId())))
            .executes((source) -> {
                execute(source.getSource());
                return 0;
            }));
        dispatcher.register(CommandManager.literal("botinfo")
            .requires((source) -> AutoWhitelist.CONFIG.admins().stream().anyMatch(v -> Objects.equals(v, source.getUser().getId())))
            .executes((source) -> {
                execute(source.getSource());
                return 0;
            }));
    }

    protected static void execute(DiscordCommandSource source) {
        analyzeTimings("BotStatusCommand#execute", () -> {

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle("Bot Status Log");
            embedBuilder.setDescription("**Bot status:** " + jda.getStatus());

            jda.getRestPing().queue(restPing -> {
                String output = "\n" + "**Gateway ping:** " + jda.getGatewayPing() + " ms" +
                    "\n" + "**Rest ping:** " + restPing + " ms";

                embedBuilder.addField("Discord timings", output, false);

                source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            });
        });
    }
}
