package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.UserLinkCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

public class UserLinkRemoveUserSubCommand extends ChatInputSubCommand<UserLinkCommand> {
    public UserLinkRemoveUserSubCommand(@NotNull UserLinkCommand parent) {
        super(parent, "user");
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        return null;
    }
}
