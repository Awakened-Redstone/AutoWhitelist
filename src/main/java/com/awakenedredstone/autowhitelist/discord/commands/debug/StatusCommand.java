package com.awakenedredstone.autowhitelist.discord.commands.debug;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.util.Debugger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

@Deprecated(forRemoval = true)
public class StatusCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        LiteralCommandNode<DiscordCommandSource> literalCommandNode = dispatcher.register(CommandManager.literal("status")
                .requires((source) -> AutoWhitelist.CONFIG.admins().stream().anyMatch(v -> Objects.equals(v, source.getUser().getId())))
                .executes((source) -> {
                    execute(source.getSource());
                    return 0;
                }));

        dispatcher.register(CommandManager.literal("info")
                .requires((source) -> AutoWhitelist.CONFIG.admins().stream().anyMatch(v -> Objects.equals(v, source.getUser().getId())))
                .executes((source) -> {
                    execute(source.getSource());
                    return 0;
                }));
    }

    protected static void execute(DiscordCommandSource source) {
        analyzeTimings("StatusCommand#execute", () -> {
            Runtime runtime = Runtime.getRuntime();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle("Status Log");

            String output1 = "**RAM: **" + (runtime.maxMemory() - runtime.freeMemory()) / 1024L / 1024L + "MB / " + runtime.maxMemory() / 1024L / 1024L + "MB" + String.format(" (%s%% free)", Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory());

            embedBuilder.setDescription(output1);

            StringBuilder output2 = new StringBuilder();
            for (Map.Entry<String, long[]> entry : Debugger.timings.entrySet()) {
                String method = entry.getKey();
                long[] times = entry.getValue();

                output2.append("\n").append("**").append(method).append(":** ").append(Debugger.formatTimings(Arrays.stream(times).min().orElse(-1))).append("/").append(Debugger.formatTimings(MathHelper.average(times))).append("/").append(Debugger.formatTimings(Arrays.stream(times).max().orElse(-1)));
            }

            embedBuilder.addField("Processing timings", output2.toString(), true);

            source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        });
    }
}
