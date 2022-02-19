package com.awakenedredstone.autowhitelist.discord.api;

import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.util.profiler.DummyRecorder;
import net.minecraft.util.profiler.Profiler;

public class AutoWhitelistAPI {
    public static AutoWhitelistAPI INSTANCE = new AutoWhitelistAPI();
    public static CommandDispatcher<DiscordCommandSource> dispatcher() {
        return AutoWhitelistAPI.INSTANCE.commandManager.dispatcher;
    }

    private final CommandManager commandManager = new CommandManager();
    private final Profiler profiler = DummyRecorder.INSTANCE.getProfiler();

    public Profiler getProfiler() {
        return this.profiler;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
