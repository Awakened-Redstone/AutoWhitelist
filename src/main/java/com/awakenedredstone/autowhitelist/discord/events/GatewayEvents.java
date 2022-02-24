package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.AutoWhitelistAPI;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.stream.Collectors;

public class GatewayEvents {

    @SubscribeEvent
    public void onMessage(MessageReceivedEvent e) {
        String prefix = AutoWhitelist.getConfigData().prefix;
        if (e.isWebhookMessage() || e.getAuthor().isBot()) return;
        Message message = e.getMessage();
        String messageRaw = message.getContentRaw().toLowerCase();
        if (messageRaw.startsWith(prefix)) {
            String command = "/".concat(messageRaw.substring(prefix.length()));
            if (message.isFromGuild()) {
                AutoWhitelistAPI.INSTANCE.getCommandManager().execute(new DiscordCommandSource(e.getMember(), message, e.getChannel(), DiscordCommandSource.CommandType.MESSAGE, e), command);
            } else {
                AutoWhitelistAPI.INSTANCE.getCommandManager().execute(new DiscordCommandSource(e.getAuthor(), message, e.getChannel(), DiscordCommandSource.CommandType.MESSAGE, e), command);
            }
        }
    }

    @SubscribeEvent
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        String command = "/".concat(event.getName()).concat(event.getOptions().stream().map(OptionMapping::getAsString).map(v -> " " + v).collect(Collectors.joining()));
        if (event.isFromGuild()) {
            AutoWhitelistAPI.INSTANCE.getCommandManager().execute(new DiscordCommandSource(event.getMember(), null, event.getChannel(), DiscordCommandSource.CommandType.SLASH_COMMAND, event), command);
        } else {
            AutoWhitelistAPI.INSTANCE.getCommandManager().execute(new DiscordCommandSource(event.getUser(), null, event.getChannel(), DiscordCommandSource.CommandType.SLASH_COMMAND, event), command);
        }
    }
}
