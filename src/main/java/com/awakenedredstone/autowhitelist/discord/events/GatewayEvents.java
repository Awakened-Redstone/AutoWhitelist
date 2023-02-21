package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.DiscordBrigadierHelper;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import net.dv8tion.jda.api.entities.Message;
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class GatewayEvents {

    @SubscribeEvent
    public void onMessage(MessageReceivedEvent e) {
        String prefix = AutoWhitelist.CONFIG.prefix();
        if (e.isWebhookMessage() || e.getAuthor().isBot()) return;
        Message message = e.getMessage();
        String messageRaw = message.getContentRaw().toLowerCase();
        if (messageRaw.startsWith(prefix)) {
            String command = "/".concat(messageRaw.substring(prefix.length()));
            if (message.isFromGuild()) {
                DiscordBrigadierHelper.INSTANCE.getCommandManager().execute(new DiscordCommandSource(e.getMember(), message, e.getChannel(), DiscordCommandSource.CommandType.MESSAGE, e), command);
            } else {
                DiscordBrigadierHelper.INSTANCE.getCommandManager().execute(new DiscordCommandSource(e.getAuthor(), message, e.getChannel(), DiscordCommandSource.CommandType.MESSAGE, e), command);
            }
        }
    }
}
