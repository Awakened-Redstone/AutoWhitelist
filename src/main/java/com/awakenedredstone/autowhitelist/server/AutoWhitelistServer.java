package com.awakenedredstone.autowhitelist.server;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.bot.Bot;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.database.SQLite;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

@Environment(EnvType.SERVER)
public class AutoWhitelistServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        new SQLite().connect();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> AutoWhitelistCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.SERVER_STOPPED.register((server -> Bot.stopBot()));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            AutoWhitelist.server = server;
            new Bot();
        }));

    }
}
