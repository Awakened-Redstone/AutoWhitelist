package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.util.Texts;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleSlashCommand extends SlashCommand {
    private final @Nullable String prefix;

    public SimpleSlashCommand(String command) {
        this(command, null);
    }

    public SimpleSlashCommand(String command, @Nullable String prefix) {
        this.prefix = prefix;
        this.name = command;
        this.help = commandDescription();
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
    }

    private String getId() {
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
}
