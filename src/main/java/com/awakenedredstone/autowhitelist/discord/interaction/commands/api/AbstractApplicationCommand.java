package com.awakenedredstone.autowhitelist.discord.interaction.commands.api;

import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.CommonResponseMessages;
import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractApplicationCommand<T extends ApplicationCommandInteractionEvent> implements DeferrableInteraction<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationCommand.class);
    protected final String name;
    protected @Nullable String description;
    protected @NotNull ApplicationCommandContexts[] contexts;
    protected @NotNull Permission[] permissions = new Permission[0];
    protected final List<ApplicationCommandOptionData> options = new ArrayList<>(0);
    protected final ApplicationCommand.Type commandType;

    protected final @Nullable String category;

    public AbstractApplicationCommand(@NotNull String name, @NotNull ApplicationCommand.Type commandType) {
        this(name, null, commandType);
    }

    public AbstractApplicationCommand(@NotNull String name, @Nullable String category, @NotNull ApplicationCommand.Type commandType) {
        this.name = name;

        this.category = category;
        this.commandType = commandType;
    }

    public abstract @NotNull Publisher<?> execute(@NotNull T event);

    public String getTranslationName() {
        if (StringUtils.isNotBlank(category)) {
            return category + "/" + name;
        }

        return name;
    }

    protected String commandDescription() {
        return Texts.translated("discord.command.description.%s".formatted(this.getTranslationName()));
    }

    protected String argumentDescription(String argument) {
        return Texts.translated("discord.command.description.%s.argument/%s".formatted(this.getTranslationName(), argument));
    }

    protected String choice(String argument, String option) {
        return Texts.translated("discord.command.option.%s.%s/%s".formatted(this.getTranslationName(), argument, option));
    }

    protected void setPermissions(Permission ...permissions) {
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public ApplicationCommandContexts[] getContexts() {
        return contexts;
    }

    public Permission[] getPermissions() {
        return permissions;
    }

    public List<ApplicationCommandOptionData> getOptions() {
        return options;
    }

    public ApplicationCommand.Type getCommandType() {
        return commandType;
    }
}
