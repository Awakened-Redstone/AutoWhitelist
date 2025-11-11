package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonObject;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.discord.old.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.duck.WhitelistCacheHolder;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.entry.implementation.CommandEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.VanillaTeamEntryAction;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.WhitelistEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.GroupEntryAction;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.PermissionEntryAction;
import com.awakenedredstone.autowhitelist.mixin.ServerLoginNetworkHandlerAccessor;
import com.awakenedredstone.autowhitelist.util.JsonUtil;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.cache.WhitelistCache;
import com.awakenedredstone.autowhitelist.whitelist.cache.WhitelistCacheEntry;
import com.mojang.authlib.GameProfile;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Whitelist;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.SERVER)
public class AutoWhitelist implements DedicatedServerModInitializer {
    public static final String MOD_ID = "autowhitelist";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final AutoWhitelistConfig CONFIG = new AutoWhitelistConfig();
    public static final File WHITELIST_CACHE_FILE = new File("whitelist-cache.json");
    private static MinecraftServer server;

    @Deprecated(forRemoval = true)
    public static void removePlayer(LinkedPlayerProfile profile) {
        if (server.getPlayerManager().getWhitelist().isAllowed(profile)) {
            server.getPlayerManager().getWhitelist().remove(new LinkedWhitelistEntry(profile));
        }

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(/*? if <1.21.9 {*//*AutoWhitelist.getServer().getCommandSource()*//*?}*/);
        }
    }

    @Deprecated(forRemoval = true)
    public static ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = /*? if <1.21.9 {*/ /*server.getOverworld(); *//*?} else {*/ server.getSpawnWorld() /*?}*/;
        return new ServerCommandSource(
          server,
          serverWorld == null ? Vec3d.ZERO :
            //? if <1.21.9 {
            /*Vec3d.of(serverWorld.getSpawnPos()),
            *///?} else {
            Vec3d.of(serverWorld.getSpawnPoint().getPos()),
            //?}
            Vec2f.ZERO,
          serverWorld, CONFIG.commandPermissionLevel, "AutoWhitelist", Text.literal("AutoWhitelist"), server, null
        );
    }

    @Deprecated(forRemoval = true)
    public static void loadWhitelistCache() {
        if (!CONFIG.enableWhitelistCache) return;

        if (!WHITELIST_CACHE_FILE.exists()) {
            try {
                getWhitelistCache().save();
            } catch (IOException e) {
                LOGGER.error("Failed to create whitelist cache!", e);
            }
        }

        try {
            getWhitelistCache().load();
        } catch (Throwable exception) {
            LOGGER.error("Failed to load whitelist cache: ", exception);
        }
    }

    @Deprecated(forRemoval = true)
    public static void updateEntryMap(List<BaseEntryAction> entries) {
        if (!DiscordClientHolder.hasTask()) return;

        RoleActionMap.clear();
        for (BaseEntryAction entry : entries) {
            for (String roleString : entry.getRoles()) {
                Optional<Role> perhapsRole = DiscordBotHelper.getRoleFromString(roleString);
                if (perhapsRole.isPresent()) {
                    RoleActionMap.register(perhapsRole.get(), entry);
                } else {
                    LOGGER.warn("Invalid role \"{}\", ignoring", roleString);
                }
            }
        }
    }

    @Override
    public void onInitializeServer() {
        CONFIG.registerListener("entries", (List<BaseEntryAction> entries) -> {
            for (BaseEntryAction entry : entries) {
                if (!entry.isValid()) {
                    AutoWhitelistConfig.getLogger().error("Failed to validate entry {}", entry);
                }
            }

            updateEntryMap(entries);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> AutoWhitelist.server = server);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AutoWhitelistCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> DiscordClientHolder.BOT_SERVICE.shutdown()));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            CONFIG.load();

            loadWhitelistCache();

            if (!(server.getPlayerManager().getWhitelist() instanceof LinkingWhitelist)) {
                AutoWhitelist.LOGGER.error("Failed to replace whitelist, the mod can not work without the custom system!");
            }

            DiscordClientHolder.queueBotInit();
            if (!server.isOnlineMode()) {
                LOGGER.warn("***** OFFLINE SERVER DETECTED! *****");
                LOGGER.warn("This mod does not offer support for offline servers!");
                LOGGER.warn("Using a whitelist on an offline server offers little to no protection");
                LOGGER.warn("AutoWhitelist may not work properly, fully or at all on an offline server");
            }
        }));

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (!AutoWhitelist.CONFIG.enableWhitelistCache) return;
            if (!AutoWhitelist.getServer().isOnlineMode()) return;
            if (!DiscordClientHolder.hasTask()) return;
            ServerLoginNetworkHandlerAccessor accessor = (ServerLoginNetworkHandlerAccessor) handler;
            GameProfile profile = accessor.getProfile();
            if (handler.getConnectionInfo() == null) return;
            //? if <1.21.9 {
            /*Text canJoin = AutoWhitelist.getServer().getPlayerManager().checkCanJoin(accessor.getConnection().getAddress(), profile);
            *///?} else {
            Text canJoin = AutoWhitelist.getServer().getPlayerManager().checkCanJoin(accessor.getConnection().getAddress(), new net.minecraft.server.PlayerConfigEntry(profile));
            //?}
            if (canJoin != null && !canJoin.equals(Text.translatable("multiplayer.autowhitelist.disconnect.not_whitelisted"))) return;

            WhitelistCacheEntry cachedEntry = getWhitelistCache().get(profile);
            if (cachedEntry == null) return;
            String discordId = cachedEntry.getProfile().getDiscordId();
            Member member = DiscordClientHolder.getCurrent().guild.get().getMemberById(Snowflake.of(discordId)).block();
            if (member == null) {
                getWhitelistCache().remove(profile);
                return;
            }

            Optional<Role> perhapsRole = DiscordBotHelper.getHighestEntryRole(member);
            if (perhapsRole.isEmpty()) return;

            Role role = perhapsRole.get();
            BaseEntryAction entry = RoleActionMap.get(role);
            if (!entry.isValid()) {
                LOGGER.error("Failed to use whitelist cache due to a broken entry {}", entry);
                return;
            }

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            LinkedPlayerProfile extendedProfile = new LinkedPlayerProfile(Stonecutter.profileId(profile), Stonecutter.profileName(profile), role.getId().asString(), discordId, CONFIG.lockTime());
            whitelist.add(new LinkedWhitelistEntry(extendedProfile));
            entry.registerUser(extendedProfile);
        });
    }

    public static MinecraftServer getServer() {
        return server;
    }

    @Deprecated(forRemoval = true)
    public static WhitelistCache getWhitelistCache() {
        return ((WhitelistCacheHolder) getServer().getPlayerManager()).autoWhitelist$getWhitelistCache();
    }

    public static Identifier id(String ...path) {
        return Identifier.of(MOD_ID, String.join(path, "/"));
    }

    @Deprecated(forRemoval = true)
    static {
        BaseEntryAction.register(WhitelistEntryAction.ID, WhitelistEntryAction.CODEC);
        BaseEntryAction.register(VanillaTeamEntryAction.ID, VanillaTeamEntryAction.CODEC);
        BaseEntryAction.register(CommandEntryAction.ID, CommandEntryAction.CODEC);
        if (ModData.isModLoaded("luckperms")) {
            BaseEntryAction.register(PermissionEntryAction.ID, PermissionEntryAction.CODEC);
            BaseEntryAction.register(GroupEntryAction.ID, GroupEntryAction.CODEC);
        }

        BaseEntryAction.addDataFixer(VanillaTeamEntryAction.ID, (configVersion, entryData) -> {
            if (configVersion == 1) {
                JsonObject execute = new JsonObject();
                JsonUtil.moveAndRename(entryData, execute, "team", "associate_team");

                entryData.put("execute", execute);
            }

            return entryData;
        });
        BaseEntryAction.addDataFixer(CommandEntryAction.ID, (configVersion, entryData) -> {
            if (configVersion == 1) {
                JsonObject execute = new JsonObject();
                JsonUtil.moveAndRename(entryData, execute, "addCommand", "on_add");
                JsonUtil.moveAndRename(entryData, execute, "removeCommand", "on_remove");

                entryData.put("execute", execute);
            }

            return entryData;
        });
        if (ModData.isModLoaded("luckperms")) {
            BaseEntryAction.addDataFixer(PermissionEntryAction.ID, (configVersion, entryData) -> {
                if (configVersion == 1) {
                    JsonObject execute = new JsonObject();
                    JsonUtil.move(entryData, execute, "permission");

                    entryData.put("execute", execute);
                }

                return entryData;
            });
            BaseEntryAction.addDataFixer(GroupEntryAction.ID, (configVersion, entryData) -> {
                if (configVersion == 1) {
                    JsonObject execute = new JsonObject();
                    JsonUtil.move(entryData, execute, "group");

                    entryData.put("execute", execute);
                }

                return entryData;
            });
        }
    }
}
