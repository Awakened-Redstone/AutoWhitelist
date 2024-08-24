package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.entry.CommandEntry;
import com.awakenedredstone.autowhitelist.entry.TeamEntry;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.entry.WhitelistEntry;
import com.awakenedredstone.autowhitelist.entry.luckperms.GroupEntry;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.entry.luckperms.PermissionEntry;
import com.awakenedredstone.autowhitelist.entry.serialization.JanksonOps;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.mixin.ServerLoginNetworkHandlerAccessor;
import com.awakenedredstone.autowhitelist.util.*;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCache;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.mojang.authlib.GameProfile;
/*? if >=1.19 {*/
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
/*?} else {*/
/*import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderResult;
*//*?}*/
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.JDAImpl;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command./*? if >=1.19 {*/v2/*?} else {*//*v1*//*?}*/.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
/*? if >=1.18.2 {*/
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
/*?} else {*/
/*import org.apache.logging.log4j.Logger;
*//*?}*/
import org.spongepowered.asm.mixin.Unique;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Environment(EnvType.SERVER)
public class AutoWhitelist implements DedicatedServerModInitializer {
    public static final String MOD_ID = "autowhitelist";
    public static final Logger LOGGER = Stonecutter.logger("AutoWhitelist");
    public static final Logger DATA_FIXER_LOGGER = Stonecutter.logger("AutoWhitelist Data Fixer");
    public static final AutoWhitelistConfig CONFIG;
    public static final File WHITELIST_CACHE_FILE = new File("whitelist-cache.json");
    public static final WhitelistCache WHITELIST_CACHE = new WhitelistCache(WHITELIST_CACHE_FILE);
    public static final Map<String, BaseEntry> ENTRY_MAP_CACHE = new HashMap<>();
    private static MinecraftServer server;

    static {
        CONFIG = new AutoWhitelistConfig();
    }

    public static void updateWhitelist() {
        PlayerManager playerManager = server.getPlayerManager();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) playerManager.getWhitelist();

        Collection<? extends net.minecraft.server.WhitelistEntry> entries = whitelist.getEntries();

        List<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey()).toList();

        for (GameProfile profile : profiles) {
            if (server.getUserCache() == null) {
                LOGGER.error("Failed to update whitelist, could not get user cache");
                return;
            }
            GameProfile cachedProfile = server.getUserCache().getByUuid(profile.getId()).orElse(null);
            if (cachedProfile == null) continue;

            if (!profile.getName().equals(cachedProfile.getName()) && profile instanceof ExtendedGameProfile extendedProfile) {
                getCommandSource().sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Fixing bad entry from " + profile.getName())), true);
                whitelist.add(new ExtendedWhitelistEntry(new ExtendedGameProfile(cachedProfile.getId(), cachedProfile.getName(), extendedProfile.getRole(), extendedProfile.getDiscordId(), extendedProfile.getLockedUntil())));
            }
        }

        CONFIG.entries.forEach(BaseEntry::purgeInvalid);

        for (GameProfile profile : profiles) {
            if (profile instanceof ExtendedGameProfile extended) {
                BaseEntry entry = ENTRY_MAP_CACHE.get(extended.getRole());
                if (entry.shouldUpdate(extended)) entry.updateUser(extended, entry);
            }
        }

        if (server.getPlayerManager().isWhitelistEnabled()) {
            server.kickNonWhitelistedPlayers(server.getCommandSource());
        }
    }

    public static void removePlayer(ExtendedGameProfile profile) {
        if (server.getPlayerManager().getWhitelist().isAllowed(profile)) {
            server.getPlayerManager().getWhitelist().remove(new ExtendedWhitelistEntry(profile));
            ENTRY_MAP_CACHE.get(profile.getRole()).removeUser(profile);
        }
    }

    public static ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = server.getOverworld();
        return new ServerCommandSource(server, serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO,
            serverWorld, 4, "AutoWhitelist", Stonecutter.literalText("AutoWhitelist"), server, null);
    }

    public static void loadWhitelistCache() {
        try {
            WHITELIST_CACHE.load();
        } catch (Throwable exception) {
            LOGGER.warn("Failed to load whitelist cache: ", exception);
        }
    }

    @Override
    public void onInitializeServer() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DiscordBot.stopBot(true), "JDA shutdown"));

        if (!WHITELIST_CACHE_FILE.exists()) {
            try {
                WHITELIST_CACHE.save();
            } catch (IOException e) {
                LOGGER.warn("Failed to save whitelist cache: ", e);
            }
        }

        Path oldOptions = FabricLoader.getInstance().getConfigDir().resolve("autowhitelist");
        if (oldOptions.toFile().exists() && oldOptions.resolve("autowhitelist.json").toFile().exists() && !oldOptions.resolve(".migrated").toFile().exists()) {
            try {
                JsonObject json = CONFIG.getInterpreter().load(oldOptions.resolve("autowhitelist.json").toFile());
                if (json.containsKey("whitelistScheduledVerificationSeconds")) {
                    CONFIG.updatePeriod = (short) MathHelper.clamp(json.getShort("whitelistScheduledVerificationSeconds", (short) 60), 10, 300);
                }
                if (json.containsKey("owners")) {
                    //noinspection DataFlowIssue
                    CONFIG.admins = ((JsonArray) json.get("owners")).stream().map(v -> Long.parseLong(((JsonPrimitive) v).asString())).toList();
                }
                if (json.containsKey("prefix")) {
                    CONFIG.prefix = json.get(String.class, "prefix");
                }
                if (json.containsKey("token")) {
                    CONFIG.token = json.get(String.class, "token");
                }
                if (json.containsKey("discordServerId")) {
                    CONFIG.discordServerId = Long.parseLong(json.get(String.class, "discordServerId"));
                }
                if (json.containsKey("whitelist")) {
                    JsonObject whitelist = json.getObject("whitelist");
                    //noinspection DataFlowIssue
                    whitelist.forEach((key, value) -> {
                        if (value instanceof JsonArray) {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.put("roles", value);
                            jsonObject.put("type", new JsonPrimitive(TeamEntry.ID.toString()));
                            JsonObject execute = new JsonObject();
                            execute.put("team", new JsonPrimitive(key));
                            jsonObject.put("execute", execute);

                            BaseEntry entry = BaseEntry.CODEC.parse(JanksonOps.INSTANCE, jsonObject).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/);
                            CONFIG.entries.add(entry);
                        }
                    });
                }
                CONFIG.save();
                DATA_FIXER_LOGGER.info("Successfully migrated from AutoWhitelist for Snapshots");

                try {
                    boolean newFile = oldOptions.resolve(".migrated").toFile().createNewFile();
                    if (!newFile) {
                        DATA_FIXER_LOGGER.error("Failed to create flag file, the will keep trying to migrate the old config file, please delete the old config file and restart the server");
                    }
                } catch (IOException e) {
                    DATA_FIXER_LOGGER.error("Failed to create flag file, the will keep trying to migrate the old config file, please delete the old config file and restart the server", e);
                }
            } catch (Throwable e) {
                DATA_FIXER_LOGGER.error("Failed to load old config file", e);
            }
        }

        CONFIG.<List<BaseEntry>>registerListener("entries", newEntries -> {
            ENTRY_MAP_CACHE.clear();
            if (DiscordBot.getInstance() != null && DiscordBot.guild != null) {
                for (BaseEntry newEntry : newEntries) {
                    for (String roleString : newEntry.getRoles()) {
                        Role role = DiscordBotHelper.getRoleFromString(roleString);
                        if (role != null) {
                            ENTRY_MAP_CACHE.put(role.getId(), newEntry);
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            AutoWhitelist.server = server;
            CONFIG.load();
            loadWhitelistCache();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, /*? if >=1.19 {*/registryAccess,/*?}*/ environment) -> AutoWhitelistCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> DiscordBot.stopBot(false)));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
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

        /*? if >=1.19 {*/Placeholders/*?} else {*//*PlaceholderAPI*//*?}*/.register(
          AutoWhitelist.id("prefix"),
          (ctx/*? if >=1.19 {*/, arg/*?}*/) -> PlaceholderResult.value(Stonecutter.literalText(AutoWhitelist.CONFIG.prefix))
        );

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (!AutoWhitelist.CONFIG.enableWhitelistCache) return;
            if (!AutoWhitelist.getServer().isOnlineMode()) return;
            if (DiscordBot.jda == null) return;
            if (DiscordBot.guild == null) return;
            ServerLoginNetworkHandlerAccessor accessor = (ServerLoginNetworkHandlerAccessor) handler;
            GameProfile profile = accessor.getProfile();
            if (handler.getConnectionInfo() == null) return;
            if (AutoWhitelist.getServer().getPlayerManager().checkCanJoin(accessor.getConnection().getAddress(), profile) != null) return;

            WhitelistCacheEntry cachedEntry = AutoWhitelist.WHITELIST_CACHE.get(profile);
            if (cachedEntry == null) return;
            String discordId = cachedEntry.getProfile().getDiscordId();
            Member member = DiscordBot.guild.getMemberById(discordId);
            if (member == null) {
                AutoWhitelist.WHITELIST_CACHE.remove(profile);
                return;
            }
            List<Role> roles = member.getRoles();

            Optional<String> roleOptional = getTopRole(roles);
            if (roleOptional.isEmpty()) return;
            String role = roleOptional.get();

            BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(role);
            if (hasException(entry::assertSafe, "Failed to use whitelist cache due to a broken entry, please check your config file!")) return;

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), role, discordId, CONFIG.lockTime());
            whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
            entry.registerUser(profile);
        });
    }

    @Unique
    private Optional<String> getTopRole(List<Role> roles) {
        for (Role role : roles) {
            if (AutoWhitelist.ENTRY_MAP_CACHE.containsKey(role.getId())) {
                return Optional.of(role.getId());
            }
        }

        return Optional.empty();
    }

    @Unique
    private boolean hasException(Runnable task, String message) {
        try {
            task.run();
            return false;
        } catch (Throwable e) {
            LOGGER.error(message, e);
            return true;
        }
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static Identifier id(String path) {
        return Stonecutter.identifier(MOD_ID, path);
    }

    static {
        BaseEntry.register(WhitelistEntry.ID, WhitelistEntry.CODEC);
        BaseEntry.register(TeamEntry.ID, TeamEntry.CODEC);
        BaseEntry.register(CommandEntry.ID, CommandEntry.CODEC);
        if (ModData.isModLoaded("luckperms")) {
            BaseEntry.register(PermissionEntry.ID, PermissionEntry.CODEC);
            BaseEntry.register(GroupEntry.ID, GroupEntry.CODEC);
        }

        BaseEntry.addDataFixer(TeamEntry.ID, (configVersion, entryData) -> {
            if (configVersion == 1) {
                JsonObject execute = new JsonObject();
                JsonUtil.moveAndRename(entryData, execute, "team", "associate_team");

                entryData.put("execute", execute);
            }

            return entryData;
        });
        BaseEntry.addDataFixer(CommandEntry.ID, (configVersion, entryData) -> {
            if (configVersion == 1) {
                JsonObject execute = new JsonObject();
                JsonUtil.moveAndRename(entryData, execute, "addCommand", "on_add");
                JsonUtil.moveAndRename(entryData, execute, "removeCommand", "on_remove");

                entryData.put("execute", execute);
            }

            return entryData;
        });
        if (ModData.isModLoaded("luckperms")) {
            BaseEntry.addDataFixer(PermissionEntry.ID, (configVersion, entryData) -> {
                if (configVersion == 1) {
                    JsonObject execute = new JsonObject();
                    JsonUtil.move(entryData, execute, "permission");

                    entryData.put("execute", execute);
                }

                return entryData;
            });
            BaseEntry.addDataFixer(GroupEntry.ID, (configVersion, entryData) -> {
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
