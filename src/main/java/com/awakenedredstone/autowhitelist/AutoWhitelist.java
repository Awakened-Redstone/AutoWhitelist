package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfigSettings;
import com.awakenedredstone.moondust.config.api.Config;
import com.awakenedredstone.autowhitelist.entry.Entry;
import com.awakenedredstone.autowhitelist.entry.EntryActions;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.SERVER)
public class AutoWhitelist implements DedicatedServerModInitializer {
    public static final String MOD_ID = "autowhitelist";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final Config<AutoWhitelistConfig, AutoWhitelistConfigSettings> CONFIG = new Config<>(new AutoWhitelistConfigSettings());
    public static /*? if <1.21.11 {*//*Integer*//*?} else {*/LevelBasedPermissionSet/*?}*/ earlyConfigPermissionLevel;

    @Override
    public void onInitializeServer() {
        EntryActions.init();

        CONFIG.listen(config -> {
            earlyConfigPermissionLevel = config.whitelist.commandPermissionLevel;
            for (Entry entry : config.whitelist.allow) {
                for (EntryAction<?> entryAction : entry.actions()) {
                    entryAction.assertValid(true);
                }
            }
            earlyConfigPermissionLevel = null;

            if (DiscordClientHolder.hasTask()) RoleEntryMap.reload(config.whitelist.allow);
        });

        ServerDetails.registerEvents();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AutoWhitelistCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> DiscordClientHolder.BOT_SERVICE.shutdown()));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            if (CONFIG.tryLoad()) DiscordClientHolder.queue();
        }));

        ServerLoginConnectionEvents.QUERY_START.register(new QueryEventHandler());
    }

    @Contract("_ -> new")
    public static @NotNull Identifier id(String ...path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, String.join("/", path));
    }

    public static AutoWhitelistConfig config() {
        return CONFIG.get();
    }

    public static boolean useGuyser() {
        return config().guyserSupport.useGuyser;
    }
}
