package com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl;

import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class ChatInputSubCommandGroup<T extends ChatInputApplicationCommand> {
    protected final String name;
    protected final T parent;
    protected String description;
    protected final List<ChatInputSubCommand<T>> subCommands = new ArrayList<>(0);

    public ChatInputSubCommandGroup(@NotNull T parent, @NotNull String command) {
        this.name = command;
        this.parent = parent;
        this.description = commandDescription();
    }

    public ApplicationCommandOptionData asOption() {
        return ApplicationCommandOptionData.builder()
          .name(name)
          .description(description)
          .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
          .options(this.subCommands.stream().map(ChatInputSubCommand::asOption).toList())
          .build();
    }

    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        // TODO
        return Mono.empty();
    }

    protected String getTranslationName() {
        return parent.getTranslationName() + "/" + name;
    }

    protected String commandDescription() {
        return Texts.translated("discord.command.description.%s".formatted(this.getTranslationName()));
    }

    protected String argumentDescription(String argument) {
        return Texts.translated("discord.command.description.%s.argument/%s".formatted(this.getTranslationName(), argument));
    }

    protected String choice(String name, String option) {
        return Texts.translated("discord.command.option.%s.%s/%s".formatted(this.getTranslationName(), name, option));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ChatInputSubCommand<T>> getSubCommands() {
        return subCommands;
    }
}
