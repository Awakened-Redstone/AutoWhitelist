package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.viewlink.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import com.awakenedredstone.autowhitelist.util.Optioning;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;

import java.util.List;

public class ViewLinkUsernameSubCommand extends ChatInputSubCommand<ViewLinkChatCommand> {
    public ViewLinkUsernameSubCommand(@NotNull ViewLinkChatCommand parent) {
        super(parent, "username");

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
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        var username = Optioning.getOptionAsString(options, "username").orElseThrow();

        return event.deferReply().then(LinkInfoCommand.execute(event, username));
    }
}
