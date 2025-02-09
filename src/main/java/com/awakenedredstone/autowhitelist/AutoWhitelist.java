package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonObject;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.entry.implementation.CommandEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.VanillaTeamEntryAction;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.WhitelistEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.GroupEntryAction;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.PermissionEntryAction;
import com.awakenedredstone.autowhitelist.mixin.ServerLoginNetworkHandlerAccessor;
import com.awakenedredstone.autowhitelist.util.JsonUtil;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCache;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
    public static final Logger DATA_FIXER_LOGGER = LoggerFactory.getLogger("AutoWhitelist Data Fixer");
    public static final AutoWhitelistConfig CONFIG = new AutoWhitelistConfig();
    public static final File WHITELIST_CACHE_FILE = new File("whitelist-cache.json");
    public static final WhitelistCache WHITELIST_CACHE = new WhitelistCache(WHITELIST_CACHE_FILE);
    private static MinecraftServer server;

    public static void removePlayer(ExtendedGameProfile profile) {
        if (server.getPlayerManager().getWhitelist().isAllowed(profile)) {
            server.getPlayerManager().getWhitelist().remove(new ExtendedWhitelistEntry(profile));
        }

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(AutoWhitelist.getServer().getCommandSource());
        }
    }

    public static ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = server.getOverworld();
        return new ServerCommandSource(
          server, serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO,
          serverWorld, CONFIG.commandPermissionLevel, "AutoWhitelist", Text.literal("AutoWhitelist"), server, null
        );
    }

    public static void loadWhitelistCache() {
        try {
            WHITELIST_CACHE.load();
        } catch (Throwable exception) {
            LOGGER.error("Failed to load whitelist cache: ", exception);
        }
    }

    public static void updateEntryMap(List<BaseEntryAction> entries) {
        if (DiscordBot.getInstance() == null || DiscordBot.getGuild() == null) return;

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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DiscordBot.stopBot(true), "JDA shutdown"));

        if (!WHITELIST_CACHE_FILE.exists()) {
            try {
                WHITELIST_CACHE.save();
            } catch (IOException e) {
                LOGGER.error("Failed to save whitelist cache: ", e);
            }
        }

        CONFIG.registerListener("entries", (List<BaseEntryAction> entries) -> {
            for (BaseEntryAction entry : entries) {
                if (!entry.isValid()) {
                    AutoWhitelistConfig.getLogger().error("Failed to validate entry {}", entry);
                }
            }

            updateEntryMap(entries);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            AutoWhitelist.server = server;
            loadWhitelistCache();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AutoWhitelistCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> DiscordBot.stopBot(false)));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            CONFIG.load();
            if (!(server.getPlayerManager().getWhitelist() instanceof ExtendedWhitelist)) {
                AutoWhitelist.LOGGER.error("Failed to replace whitelist, the mod can not work without the custom system!");
            }

            DiscordBot.startInstance();
            if (!server.isOnlineMode()) {
                LOGGER.warn("***** OFFLINE SERVER DETECTED! *****");
                LOGGER.warn("This mod does not offer support for offline servers!");
                LOGGER.warn("Using a whitelist on an offline server offers little to no protection");
                LOGGER.warn("AutoWhitelist may not work properly, fully or at all on an offline server");
            }
        }));

        //Placeholders.register(AutoWhitelist.id("whitelisted_players"), (ctx, arg) -> PlaceholderResult.value(Text.literal()));

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (!AutoWhitelist.CONFIG.enableWhitelistCache) return;
            if (!AutoWhitelist.getServer().isOnlineMode()) return;
            if (!DiscordBot.botExists()) return;
            if (DiscordBot.getGuild() == null) return;
            ServerLoginNetworkHandlerAccessor accessor = (ServerLoginNetworkHandlerAccessor) handler;
            GameProfile profile = accessor.getProfile();
            if (handler.getConnectionInfo() == null) return;
            Text canJoin = AutoWhitelist.getServer().getPlayerManager().checkCanJoin(accessor.getConnection().getAddress(), profile);
            if (canJoin != null && !canJoin.equals(Text.translatable("multiplayer.disconnect.not_whitelisted"))) return;

            WhitelistCacheEntry cachedEntry = AutoWhitelist.WHITELIST_CACHE.get(profile);
            if (cachedEntry == null) return;
            String discordId = cachedEntry.getProfile().getDiscordId();
            Member member = DiscordBot.getGuild().getMemberById(discordId);
            if (member == null) {
                AutoWhitelist.WHITELIST_CACHE.remove(profile);
                return;
            }

            Optional<Role> role = DiscordBotHelper.getHighestEntryRole(member);
            if (role.isEmpty()) return;

            BaseEntryAction entry = RoleActionMap.get(role.get());
            if (!entry.isValid()) {
                LOGGER.error("Failed to use whitelist cache due to a broken entry {}", entry);
                return;
            }

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), role.get().getId(), discordId, CONFIG.lockTime());
            whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
            entry.registerUser(extendedProfile);
        });
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

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
