package com.awakenedredstone.autowhitelist.discord.commands.developer;

import com.jagrosh.jdautilities.command.Command;

public abstract class DeveloperCommand extends Command {

    public DeveloperCommand() {
        this.category = new Category("Developer tools | Debugging");
        this.guildOnly = false;
        this.ownerCommand = true;
    }
}
