package com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.AbstractApplicationCommand;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public abstract class ChatInputApplicationCommand extends AbstractApplicationCommand<ChatInputInteractionEvent> {
    public ChatInputApplicationCommand(@NotNull String command, @Nullable String category) {
        super(command, category, ApplicationCommand.Type.CHAT_INPUT);

        this.description = commandDescription();
        this.contexts = new ApplicationCommandContexts[]{ApplicationCommandContexts.GUILD};
    }

    public ChatInputApplicationCommand(@NotNull String command) {
        super(command, ApplicationCommand.Type.CHAT_INPUT);

        this.description = commandDescription();
        this.contexts = new ApplicationCommandContexts[]{ApplicationCommandContexts.GUILD};
    }

    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        return Mono.empty();
    }

    protected ApplicationCommandOptionChoiceData simpleOptionChoice(String value) {
        return ApplicationCommandOptionChoiceData.builder().name(value).value(value).build();
    }
}
