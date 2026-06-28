package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.StatusCommandMessages;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class StatusCommand extends ChatInputApplicationCommand {
    public StatusCommand() {
        super("status", "admin");

        this.permissions = new Permission[]{Permission.KICK_MEMBERS};

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("server")
            .description(argumentDescription("server"))
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        );

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("whitelist")
            .description(argumentDescription("whitelist"))
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        );

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("bot")
            .description(argumentDescription("bot"))
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        );

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("minecraft")
            .description(argumentDescription("minecraft"))
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        );

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("config")
            .description(argumentDescription("config"))
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        );
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        Optional<String> command = event.getOptions().stream().map(ApplicationCommandInteractionOption::getName).findFirst();
        return event.deferReply()
          .then(
            command
              .map(name -> event.editReply(ResponseMessage.buildEditSpec(AutoWhitelist.id("status", name))))
              .orElseGet(Mono::empty)
          );
    }

    static {
        StatusCommandMessages.init();
    }
}
