package com.awakenedredstone.autowhitelist.server;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.lang.JigsawLanguage;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.config;
import static com.awakenedredstone.autowhitelist.lang.JigsawLanguage.translations;

@Environment(EnvType.SERVER)
public class AutoWhitelistServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> AutoWhitelistCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> Bot.stopBot()));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            AutoWhitelist.server = server;
            new SQLite().connect();

            try {
                {
                    InputStream inputStream = AutoWhitelistServer.class.getResource("/messages.json").openStream();
                    JigsawLanguage.load(inputStream, translations::put);
                }
                File file = new File(config.getConfigDirectory(), "AutoWhitelist-assets/messages.json");
                File folder = new File(config.getConfigDirectory(), "AutoWhitelist-assets");
                if (!folder.exists()) {
                    folder.mkdir();
                }
                if (!file.exists()) {
                    Files.copy(AutoWhitelistServer.class.getResource("/messages.json").openStream(), file.toPath());
                }

                InputStream inputStream = Files.newInputStream(file.toPath());
                JigsawLanguage.load(inputStream, translations::put);
            } catch (IOException ignored) {
            }

            Thread thread = new Thread(new Bot());
            thread.setName("AutoWhitelist Bot");
            thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(AutoWhitelist.LOGGER));
            thread.setDaemon(true);
            thread.start();
        }));

    }
}
