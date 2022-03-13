package com.awakenedredstone.autowhitelist.discord.commands.developer;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
//import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Objects;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class ServerStatusCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("serverstatus")
                .requires((source) -> Arrays.stream(AutoWhitelist.getConfigData().owners).anyMatch(v -> Objects.equals(v, source.getUser().getId())) || source.getUser().getId().equals("387745099204919297"))
                .executes((source) -> {
                    execute(source.getSource());
                    return 0;
                }));
        dispatcher.register(CommandManager.literal("serverinfo")
                .requires((source) -> Arrays.stream(AutoWhitelist.getConfigData().owners).anyMatch(v -> Objects.equals(v, source.getUser().getId())) || source.getUser().getId().equals("387745099204919297"))
                .executes((source) -> {
                    execute(source.getSource());
                    return 0;
                }));

//        CommandDataImpl command = new CommandDataImpl("serverStatus", new TranslatableText("command.description.serverStatus").getString());
//        jda.upsertCommand(command).queue();
    }

    protected static void execute(DiscordCommandSource source) {
        analyzeTimings("ServerStatusCommand#execute", () -> {
            MinecraftServer server = AutoWhitelist.server;
            PlayerManager playerManager = server.getPlayerManager();

            long l = Util.getMeasuringTimeMs() - server.getTimeReference();
            double MSPT = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
            double TPS = 1000.0D / Math.max(50, MSPT);
            double MAX_POSSIBLE_TPS = 1000.0D / MSPT;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle("Server Status Log");
            embedBuilder.setDescription("**Server status:** " + (getServerStatus(server).equals("Running.") ? (l > 2000L ? String.format("Running %sms behind.", l) : "Running.") : getServerStatus(server)));

            String output = "" +
                    "\n" + "**MSPT:** " + String.format("%.2f", MSPT) + " ms" +
                    "\n" + "**TPS:** " + String.format("%.2f", TPS) +
                    "\n" + "**MAX TPS:** " + String.format("%.2f", MAX_POSSIBLE_TPS);
            embedBuilder.addField("Server timings", output, true);

            StringBuilder serverInformation = new StringBuilder();
            serverInformation.append("**Server game version:** ").append(server.getVersion()).append("\n");
            serverInformation.append("**Total whitelisted players:** ").append(playerManager.getWhitelistedNames().length).append("\n");
            serverInformation.append("**Total online players:** ").append(server.getCurrentPlayerCount()).append("/").append(server.getMaxPlayerCount());

            embedBuilder.addField("Server information", serverInformation.toString(), true);

            if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
//                ((SlashCommandInteractionEvent)source.getEvent()).replyEmbeds(embedBuilder.build()).queue();
            } else {
                source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            }
        });
    }

    private static String getServerStatus(MinecraftServer server) {
        if (server.isStopped()) return "Stopped.";
        if (server.isRunning()) return "Running.";
        return "Unknown.";
    }
}
