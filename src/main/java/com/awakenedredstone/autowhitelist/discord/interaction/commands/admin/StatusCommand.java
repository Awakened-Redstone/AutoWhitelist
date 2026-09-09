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

        this.setPermissions(Permission.KICK_MEMBERS);

        this.options.add(option("minecraft"));
        this.options.add(option("networking"));
        this.options.add(option("whitelist"));
        this.options.add(option("cache"));
        this.options.add(option("bot"));
        this.options.add(option("config"));
    }

    private ApplicationCommandOptionData option(String name) {
        return ApplicationCommandOptionData.builder()
          .name(name)
          .description(argumentDescription(name))
          .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
          .build();
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
