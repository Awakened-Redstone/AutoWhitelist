package com.awakenedredstone.autowhitelist.discord.commands.developer;

import com.awakenedredstone.autowhitelist.util.Debugger;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.minecraft.util.math.MathHelper;
import oshi.SystemInfo;
import oshi.hardware.Processor;

import java.util.Arrays;
import java.util.Map;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class StatusCommand extends DeveloperCommand {

    public StatusCommand() {
        this.name = "status";
        this.help = "Shows the system status.";
        this.aliases = new String[]{"info"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE) return;
        analyzeTimings("StatusCommand#execute", () -> {
            Runtime runtime = Runtime.getRuntime();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle("Status Log");

            long[] processorsLoad;
            try {
                processorsLoad = Arrays.stream(new SystemInfo().getHardware().getProcessors()).map(Processor::getLoad).mapToLong(v -> v.longValue() * 100L).toArray();
            } catch (UnsupportedOperationException exception) {
                processorsLoad = new long[]{-1};
            }
            String output1 = "" +
                    "\n" + "**CPU: **" + (processorsLoad[0] != -1L ? MathHelper.average(processorsLoad) + "%" : "Unknown") +
                    "\n" + "**RAM: **" + (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L + "MB / " + runtime.totalMemory() / 1024L / 1024L + "MB" + String.format(" (%s%% free)", Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory());

            embedBuilder.setDescription(output1);

            StringBuilder output2 = new StringBuilder();
            for (Map.Entry<String, long[]> entry : Debugger.timings.entrySet()) {
                String method = entry.getKey();
                long[] times = entry.getValue();

                output2.append("\n").append("**").append(method).append(":** ").append(Debugger.formatTimings(Arrays.stream(times).min().orElse(-1))).append("/").append(Debugger.formatTimings(MathHelper.average(times))).append("/").append(Debugger.formatTimings(Arrays.stream(times).max().orElse(-1)));
            }

            embedBuilder.addField("Processing timings", output2.toString(), true);
            event.getChannel().sendMessage(embedBuilder.build()).queue();
        });
    }
}
