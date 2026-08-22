package com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ChatInputSubCommandGroup<T extends ChatInputApplicationCommand> extends ChatInputSubCommand<T> {
    protected final List<ChatInputSubCommand<T>> subCommands = new ArrayList<>(0);
    protected final Map<String, ChatInputSubCommand<T>> subCommandMap = new HashMap<>();

    public ChatInputSubCommandGroup(@NotNull T parent, @NotNull String command) {
        super(parent, command);
        this.type = Type.SUB_COMMAND_GROUP;
    }

    protected void addSubCommandOptions() {
        for (var subCommand : this.subCommands) {
            this.options.add(subCommand.asOption());
            this.subCommandMap.put(subCommand.getName(), subCommand);
        }
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        if (options.isEmpty()) {
            return Mono.error(new IllegalArgumentException("No option was provided for a sub-command group"));
        }

        if (options.size() > 1) {
            return Mono.error(new IllegalArgumentException("Too many options were provided for a sub-command group"));
        }

        // If there is no option there is no subcommand
        // subcommands only come as a single option
        var option = options.getFirst();
        var type = option.getType();
        String optionName = option.getName();
        if (type == Type.SUB_COMMAND || type == Type.SUB_COMMAND_GROUP) {
            var handler = subCommandMap.get(optionName);
            if (handler.type != type) {
                return Mono.error(new IllegalStateException("Handler type does not match the received type"));
            }

            return handler.execute(event, option.getOptions());
        }

        return Mono.error(new IllegalArgumentException("The provided option \"%s\" does not map to any registered sub-command".formatted(optionName)));
    }

    public List<ChatInputSubCommand<T>> getSubCommands() {
        return subCommands;
    }
}
