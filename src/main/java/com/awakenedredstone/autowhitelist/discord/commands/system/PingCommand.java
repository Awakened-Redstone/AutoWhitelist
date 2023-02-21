package com.awakenedredstone.autowhitelist.discord.commands.system;

import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.mojang.brigadier.CommandDispatcher;

import java.time.temporal.ChronoUnit;

public class PingCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ping").executes((source) -> {
            execute(source.getSource());
            return 0;
        }));

//        CommandData command = new CommandDataImpl("test", Text.translatable("command.description.test").markdownFormatted());
//        jda.upsertCommand(command).queue();
    }

    public static void execute(DiscordCommandSource source) {
        if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
            /*((SlashCommandInteractionEvent)source.getEvent()).deferReply().queue(r -> {
                r.editOriginal("Ping: ...").queue(m -> {
                    long ping = ((SlashCommandInteractionEvent)source.getEvent()).getInteraction().getTimeCreated().until(m.getTimeCreated(), ChronoUnit.MILLIS);
                    m.editMessage("Ping: " + ping  + "ms | Websocket: " + m.getJDA().getGatewayPing() + "ms").queue();
                });
            });*/
        } else {
            source.getChannel().sendMessage("Ping: ...").queue(m -> {
                long ping = source.getMessage().getTimeCreated().until(m.getTimeCreated(), ChronoUnit.MILLIS);
                m.editMessage("Ping: " + ping + "ms | Websocket: " + m.getJDA().getGatewayPing() + "ms").queue();
            });
        }
    }
}
