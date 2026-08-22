package com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl;

import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class ChatInputSubCommand<T extends ChatInputApplicationCommand> {
    protected final String name;
    protected final T parent;
    protected String description;
    protected Type type;
    protected final List<ApplicationCommandOptionData> options = new ArrayList<>(0);

    public ChatInputSubCommand(@NotNull T parent, @NotNull String name) {
        this.name = name;
        this.parent = parent;
        this.description = commandDescription();
        this.type = Type.SUB_COMMAND;
    }

    public abstract @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options);

    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        return Mono.empty();
    }

    public ApplicationCommandOptionData asOption() {
        return ApplicationCommandOptionData.builder()
          .name(name)
          .description(description)
          .type(this.type.getValue())
          .options(options)
          .build();
    }

    protected String getTranslationName() {
        return parent.getTranslationName() + "." + name;
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

    public List<ApplicationCommandOptionData> getOptions() {
        return options;
    }
}
