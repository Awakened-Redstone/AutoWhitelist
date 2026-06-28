package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class UserLinkRemoveSubCommand extends ChatInputSubCommand<UserLinkCommand> {
    public UserLinkRemoveSubCommand(@NotNull UserLinkCommand parent) {
        super(parent, "modify");

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("user")
            .description(argumentDescription("user"))
            .type(ApplicationCommandOption.Type.USER.getValue())
            .required(true)
            .build()
        );

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("username")
            .description(argumentDescription("username"))
            .autocomplete(true)
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build()
        );
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        // TODO
        return Mono.empty();
    }
}
