package com.awakenedredstone.autowhitelist.discord.commands.api;

import com.awakenedredstone.autowhitelist.util.Texts;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractApplicationCommand<T extends ApplicationCommandInteractionEvent> {
    protected final String name;
    protected String description;
    protected ApplicationCommandContexts[] contexts;
    protected Permission[] permissions = new Permission[0];
    protected final List<ApplicationCommandOptionData> options = new ArrayList<>(0);
    protected final ApplicationCommand.Type commandType;

    protected final @Nullable String prefix;

    public AbstractApplicationCommand(String command, ApplicationCommand.Type commandType) {
        this(command, null, commandType);
    }

    public AbstractApplicationCommand(String command, @Nullable String prefix, ApplicationCommand.Type commandType) {
        this.name = command;
        this.description = commandDescription();
        this.contexts = new ApplicationCommandContexts[]{ApplicationCommandContexts.GUILD};

        this.prefix = prefix;
        this.commandType = commandType;
    }

    public abstract void execute(T event);

    protected String getId() {
        if (StringUtils.isNotBlank(prefix)) {
            return prefix + "/" + name;
        }

        return name;
    }

    protected String commandDescription() {
        return Texts.translated("discord.command.description.%s".formatted(this.getId()));
    }

    protected String argumentText(String argument) {
        return Texts.translated("discord.command.description.%s.argument/%s".formatted(this.getId(), argument));
    }

    protected String choice(String argument, String option) {
        return Texts.translated("discord.command.option.%s.%s/%s".formatted(this.getId(), argument, option));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
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
