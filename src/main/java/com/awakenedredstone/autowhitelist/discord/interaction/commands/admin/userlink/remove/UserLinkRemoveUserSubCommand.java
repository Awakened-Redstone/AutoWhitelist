package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.UserLinkCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;

import java.util.List;

public class UserLinkRemoveUserSubCommand extends ChatInputSubCommand<UserLinkCommand> {
    public UserLinkRemoveUserSubCommand(@NotNull UserLinkCommand parent) {
        super(parent, "user");
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        return null;
    }
}
