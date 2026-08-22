package com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.AbstractApplicationCommand;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ChatInputApplicationCommand extends AbstractApplicationCommand<ChatInputInteractionEvent> {
    protected final List<ChatInputSubCommand<?>> subCommands = new ArrayList<>(0);
    protected final Map<String, ChatInputSubCommand<?>> subCommandMap = new HashMap<>();

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

    protected void addSubCommandOptions() {
        for (var subCommand : this.subCommands) {
            this.options.add(subCommand.asOption());
            this.subCommandMap.put(subCommand.getName(), subCommand);
        }
    }

    protected @NonNull Publisher<?> handleSubCommands(@NonNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        // If there is no option there is no subcommand
        // subcommands only come as a single option
        if (options.size() == 1) {
            var option = options.getFirst();
            var type = option.getType();
            if (type == Type.SUB_COMMAND || type == Type.SUB_COMMAND_GROUP) {
                var handler = subCommandMap.get(option.getName());
                if (handler.type != type) {
                    return Mono.error(new IllegalStateException("Handler type does not match the received type"));
                }

                return handler.execute(event, option.getOptions());
            }
        }

        // Return mono empty if no handler or subcommand was found
        return Mono.empty();
    }

    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        return Mono.empty();
    }

    protected ApplicationCommandOptionChoiceData simpleOptionChoice(String value) {
        return ApplicationCommandOptionChoiceData.builder().name(value).value(value).build();
    }
}
