package com.awakenedredstone.autowhitelist.discord.commands.api;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    public static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    @SuppressWarnings("rawtypes") // Use raw type for the JVM to auto cast the event type
    private final Map<Pair<ApplicationCommand.Type, String>, AbstractApplicationCommand> commands = new HashMap<>();

    public void register(AbstractApplicationCommand<?> command) {
        commands.put(new Pair<>(command.getCommandType(), command.getName()), command);
    }

    public void registerToDiscord(GatewayDiscordClient client) {
        long applicationId = client.rest().getApplicationId().blockOptional().orElseThrow();
        List<ApplicationCommandRequest> commandRequests = new ArrayList<>();

        commands.forEach((name, command) -> {
            var request = ApplicationCommandRequest.builder()

              .build();

            commandRequests.add(request);
        });

        client.rest().getApplicationService().bulkOverwriteGuildApplicationCommand(applicationId, AutoWhitelist.CONFIG.discordServerId, commandRequests).subscribe();
    }

    @SuppressWarnings("unchecked")
    public void execute(ApplicationCommandInteractionEvent event) {
        try {
            var command = commands.get(new Pair<>(event.getCommandType(), event.getCommandName()));
            if (command != null) {
                command.execute(event);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle interaction!", e);
        }
    }
}
