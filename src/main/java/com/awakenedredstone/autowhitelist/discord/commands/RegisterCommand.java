package com.awakenedredstone.autowhitelist.discord.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.commands.api.AbstractApplicationCommand;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.jetbrains.annotations.NotNull;

public class RegisterCommand extends AbstractApplicationCommand<ChatInputInteractionEvent> {
    public RegisterCommand() {
        super("register", ApplicationCommand.Type.CHAT_INPUT);
    }

    public void execute() {

    }

    @Override
    public void execute(ChatInputInteractionEvent event) {
        Member member = event.getUser().asMember(Snowflake.of(AutoWhitelist.CONFIG.discordServerId)).block();
        if (member == null) {
        }
        @NotNull String username = event.getOptionAsString("username").orElseThrow();
        boolean geyser = event.getOptionAsString("account_type").orElse("java").equalsIgnoreCase("bedrock");
    }
}
