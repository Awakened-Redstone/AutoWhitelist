package com.awakenedredstone.autowhitelist.discord.commands;

import com.awakenedredstone.autowhitelist.discord.commands.api.AbstractApplicationCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
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
        @NotNull User member = event.getUser();
        @NotNull String username = event.getOptionAsString("username").orElseThrow();
        boolean geyser = event.getOptionAsString("account_type").orElse("java").equalsIgnoreCase("bedrock");
    }
}
