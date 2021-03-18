package com.awakenedredstone.autowhitelist.discord.commands.developer;

import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;

import static com.awakenedredstone.autowhitelist.discord.Bot.jda;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class BotStatusCommand extends DeveloperCommand {

    public BotStatusCommand() {
        super();
        this.name = "botStatus";
        this.help = "Shows the bot status.";
        this.aliases = new String[]{"botInfo"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE) return;
        analyzeTimings("BotStatusCommand#execute", () -> {

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle("Bot Status Log");
            embedBuilder.setDescription("**Bot status:** " + jda.getStatus().toString());

            jda.getRestPing().queue(restPing -> {
                String output ="\n" + "**Gateway ping:** " + jda.getGatewayPing() + " ms" +
                        "\n" + "**Rest ping:** " + restPing  + " ms";

                embedBuilder.addField("Discord timings", output, false);
                event.getChannel().sendMessage(embedBuilder.build()).queue();
            });
        });
    }
}
