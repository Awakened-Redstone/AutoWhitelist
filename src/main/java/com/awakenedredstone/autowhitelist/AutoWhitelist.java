package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.entry.CommandEntry;
import com.awakenedredstone.autowhitelist.entry.TeamEntry;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.entry.WhitelistEntry;
import com.awakenedredstone.autowhitelist.entry.luckperms.GroupEntry;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.entry.luckperms.PermissionEntry;
import com.awakenedredstone.autowhitelist.entry.serialization.JanksonOps;
import com.awakenedredstone.autowhitelist.mixin.ServerLoginNetworkHandlerAccessor;
import com.awakenedredstone.autowhitelist.util.JsonUtil;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.util.NullUtils;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Whitelist;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.SERVER)
public class AutoWhitelist implements DedicatedServerModInitializer {
    public static final String MOD_ID = "autowhitelist";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final Logger DATA_FIXER_LOGGER = LoggerFactory.getLogger("AutoWhitelist Data Fixer");
    public static final AutoWhitelistConfig CONFIG = new AutoWhitelistConfig();
    public static final File WHITELIST_CACHE_FILE = new File("whitelist-cache.json");
    public static final WhitelistCache WHITELIST_CACHE = new WhitelistCache(WHITELIST_CACHE_FILE);
    public static final Map<String, BaseEntry> ENTRY_MAP_CACHE = new HashMap<>();
    private static MinecraftServer server;

    public static void removePlayer(ExtendedGameProfile profile) {
        if (server.getPlayerManager().getWhitelist().isAllowed(profile)) {
            BaseEntry entry = ENTRY_MAP_CACHE.get(profile.getRole());
            entry.assertValid();
            entry.removeUser(profile);
            server.getPlayerManager().getWhitelist().remove(new ExtendedWhitelistEntry(profile));
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

    public static void updateEntryMap(List<BaseEntry> entries) {
        if (DiscordBot.getInstance() == null || DiscordBot.getGuild() == null) return;

        ENTRY_MAP_CACHE.clear();
        for (BaseEntry entry : entries) {
            for (String roleString : entry.getRoles()) {
                Role role = DiscordBotHelper.getRoleFromString(roleString);
                if (role != null) {
                    ENTRY_MAP_CACHE.put(role.getId(), entry);
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

        convertConfigFromLite();

        CONFIG.registerListener("entries", (List<BaseEntry> entries) -> {
            for (BaseEntry entry : entries) {
                try {
                    entry.assertValid();
                } catch (Throwable e) {
                    AutoWhitelistConfig.getLogger().error("Failed to assert entry {}", entry, e);
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
            List<Role> roles = member.getRoles();

            Optional<String> roleOptional = DiscordDataProcessor.getTopRole(roles);
            if (roleOptional.isEmpty()) return;
            String role = roleOptional.get();

            BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(role);
            if (hasException(entry::assertValid, "Failed to use whitelist cache due to a broken entry, please check your config file!")) return;

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), role, discordId, CONFIG.lockTime());
            whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
            entry.registerUser(profile);
        });
    }

    @Deprecated(forRemoval = true, since = "1.0.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "1.1.0")
    private void convertConfigFromLite() {
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
                    CONFIG.discordServerId = Long.parseLong(NullUtils.orElse(json.get(String.class, "discordServerId"), "0"));
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
                        DATA_FIXER_LOGGER.error("Failed to create flag file, the mod will keep trying to migrate the old config file, please delete the old config file and restart the server");
                    }
                } catch (IOException e) {
                    DATA_FIXER_LOGGER.error("Failed to create flag file, the mod will keep trying to migrate the old config file, please delete the old config file and restart the server", e);
                }
            } catch (Throwable e) {
                DATA_FIXER_LOGGER.error("Failed to load old config file", e);
            }
        }
    }

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
        return Identifier.of(MOD_ID, path);
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
