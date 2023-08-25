package com.awakenedredstone.autowhitelist.discord.api;

import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.mojang.brigadier.CommandDispatcher;

public class DiscordBrigadierHelper {
    public static DiscordBrigadierHelper INSTANCE = new DiscordBrigadierHelper();
    private final CommandManager commandManager = new CommandManager();

    public static CommandDispatcher<DiscordCommandSource> dispatcher() {
        return DiscordBrigadierHelper.INSTANCE.commandManager.dispatcher;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
