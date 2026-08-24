package com.awakenedredstone.autowhitelist.discord.interaction.commands.api;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.util.Perhaps;
import com.awakenedredstone.autowhitelist.util.string.LinedStringBuilder;
import com.mojang.datafixers.util.Pair;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.BiFunction;

public class InteractionHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(InteractionHandler.class);

    private final Map<Pair<ApplicationCommand.Type, String>, AbstractApplicationCommand<?>> commands = new HashMap<>();
    private final Map<String, ButtonInteraction> interactions = new HashMap<>();

    public void registerCommand(AbstractApplicationCommand<?> command) {
        commands.put(new Pair<>(command.getCommandType(), command.getName()), command);
    }

    public void registerButton(ButtonInteraction button) {
        interactions.put(button.id.toString(), button);
    }

    public Flux<ApplicationCommandData> postCommands(GatewayDiscordClient client, long guildId) {
        long applicationId = client.rest().getApplicationId().blockOptional().orElseThrow();
        List<ApplicationCommandRequest> commandRequests = new ArrayList<>();

        commands.forEach((key, command) -> {
            long permissionBytes = 0;
            for (Permission permission : command.getPermissions()) {
                permissionBytes |= permission.getValue();
            }

            var requestBuilder = ApplicationCommandRequest.builder()
              .name(command.getName())
              .type(command.getCommandType().getValue())
              .defaultMemberPermissions(Optional.ofNullable(permissionBytes == 0 ? null : String.valueOf(permissionBytes)))
              .addAllContexts(Arrays.stream(command.getContexts()).map(ApplicationCommandContexts::getValue).toList())
              .options(command.getOptions());

            if (command.getDescription() != null) {
                requestBuilder.description(command.getDescription());
            }

            commandRequests.add(requestBuilder.build());
        });

        return client.rest().getApplicationService()
          .bulkOverwriteGuildApplicationCommand(applicationId, guildId, commandRequests)
          .doOnError(e -> LOGGER.error("Failed to update all commands", e))
          .doOnComplete(() -> LOGGER.debug("All commands updated successfully"));
    }

    // Use raw type for the JVM to auto cast the event type
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Publisher<?> onCommand(ApplicationCommandInteractionEvent event) {
        AbstractApplicationCommand command = commands.get(new Pair<>(event.getCommandType(), event.getCommandName()));
        if (command == null) {
            return Mono.empty();
        }

        return catchError(command, AbstractApplicationCommand::execute, event);
    }

    public Publisher<?> onComponent(ButtonInteractionEvent event) {
        // TODO: Modals and other components
        // TODO: extract context from id
        var interactionData = event.getInteraction().getData().data().toOptional().orElseThrow(IllegalStateException::new);
        ButtonInteraction button = interactions.get(interactionData.customId().get());

        if (button == null) {
            return Mono.empty();
        }

        return catchError(button, ButtonInteraction::execute, event);
    }

    public Publisher<?> onChatInput(ChatInputAutoCompleteEvent event) {
        try {
            var command = commands.get(new Pair<>(event.getCommandType(), event.getCommandName()));
            if (command instanceof ChatInputApplicationCommand applicationCommand) {
                return catchError(applicationCommand, ChatInputApplicationCommand::onChatInput, event);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle chat input!", e);
        }

        return Mono.empty();
    }

    private <T extends DeferrableInteractionEvent, C extends DeferrableInteraction<T>> Publisher<?> catchError(C interaction, BiFunction<C, T, Publisher<?>> execute, T event) {
        try {
            var response = execute.apply(interaction, event);
            if (response instanceof Mono<?> mono) {
                return mono.onErrorResume(throwable -> onErrorResponse(interaction, event, throwable));
            } else if (response instanceof Flux<?> flux) {
                return flux.onErrorResume(throwable -> onErrorResponse(interaction, event, throwable));
            }

            return Mono.from(response).onErrorResume(throwable -> onErrorResponse(interaction, event, throwable));
        } catch (Exception e) {
            return handleError(interaction, event, e);
        }
    }

    private static <V, T extends DeferrableInteractionEvent, C extends DeferrableInteraction<T>> @NonNull Mono<V> onErrorResponse(C interaction, T event, Throwable e) {
        return Mono.from(handleError(interaction, event, e)).then(Mono.empty());
    }

    private static <T extends DeferrableInteractionEvent, C extends DeferrableInteraction<T>> @NonNull Publisher<?> handleError(C interaction, T event, Throwable e) {
        var builder = new LinedStringBuilder("Failed to handle interaction");
        var possibleData = event.getInteraction().getData().data();
        builder.appendLine("  Handler: " + interaction.getClass().getCanonicalName());
        if (possibleData.isPresent()) {
            var data = possibleData.get();
            builder.appendLine("  Interaction data:");
            builder.appendLine("    Type: " + Perhaps.orElse(data.type().map(Interaction.Type::of).map(Enum::toString), "[unknown]"));
            builder.appendLine("    Name: " + Perhaps.orElse(data.name(), "[unknown]"));
            builder.appendLine("    Custom id: " + Perhaps.orElse(data.customId(), "[none]"));
        } else {
            builder.appendLine("  Interaction data: [none]");
        }

        LOGGER.error(builder.toString(), e);
        return interaction.onError(event, e);
    }

    private <T extends Event, C extends DeferrableInteraction<?>> Publisher<?> catchError(C interaction, BiFunction<C, T, Publisher<?>> execute, T event) {
        try {
            return execute.apply(interaction, event);
        } catch (Exception e) {
            LOGGER.error("Failed to handle interaction!", e);
            return Mono.empty();
        }
    }
}
