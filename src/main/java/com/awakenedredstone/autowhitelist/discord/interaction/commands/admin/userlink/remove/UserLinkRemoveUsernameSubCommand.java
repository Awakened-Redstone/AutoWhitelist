package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.UserLinkCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

public class UserLinkRemoveUsernameSubCommand extends ChatInputSubCommand<UserLinkCommand> {
    public UserLinkRemoveUsernameSubCommand(@NotNull UserLinkCommand parent) {
        super(parent, "username");
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        return null;
    }
}
