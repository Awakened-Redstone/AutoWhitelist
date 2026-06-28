package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.showlink.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

public class ShowLinkUsernameSubCommand extends ChatInputSubCommand<ShowLinkChatCommand> {
    public ShowLinkUsernameSubCommand(@NotNull ShowLinkChatCommand parent) {
        super(parent, "username");

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("user")
            .description(argumentDescription("user"))
            .type(ApplicationCommandOption.Type.USER.getValue())
            .required(false)
            .build()
        );
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        return null;
    }
}
