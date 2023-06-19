package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.config.Configs;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.config.compat.LuckpermsEntry;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCache;
import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.SERVER)
public class AutoWhitelist implements DedicatedServerModInitializer {
    public static final String MOD_ID = "autowhitelist";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final Configs CONFIG;
    public static final File WHITELIST_CACHE_FILE = new File("whitelist-cache.json");
    public static final WhitelistCache WHITELIST_CACHE = new WhitelistCache(WHITELIST_CACHE_FILE);
    public static MinecraftServer server;
    public static Map<String, EntryData> whitelistDataMap = new HashMap<>();

    public static void updateWhitelist() {
        PlayerManager playerManager = server.getPlayerManager();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) playerManager.getWhitelist();

        Collection<? extends WhitelistEntry> entries = whitelist.getEntries();

        List<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey()).toList();

        for (GameProfile profile : profiles) {
            GameProfile cachedProfile = server.getUserCache().getByUuid(profile.getId()).orElse(null);

            if (!profile.getName().equals(cachedProfile.getName()) && profile instanceof ExtendedGameProfile extendedProfile) {
                getCommandSource().sendFeedback(() -> Text.literal("Fixing bad entry from " + profile.getName()), true);
                whitelist.add(new ExtendedWhitelistEntry(new ExtendedGameProfile(cachedProfile.getId(), cachedProfile.getName(), extendedProfile.getRole(), extendedProfile.getDiscordId())));
            }
        }

        CONFIG.entries().forEach(EntryData::purgeInvalid);

        for (GameProfile profile : profiles) {
            if (profile instanceof ExtendedGameProfile extended) {
                EntryData entry = whitelistDataMap.get(extended.getRole());
                if (entry.shouldUpdate(extended)) entry.updateUser(extended);
            }
        }

        if (server.getPlayerManager().isWhitelistEnabled()) {
            server.kickNonWhitelistedPlayers(server.getCommandSource());
        }
    }

    public static void removePlayer(ExtendedGameProfile profile) {
        if (server.getPlayerManager().getWhitelist().isAllowed(profile)) {
            server.getPlayerManager().getWhitelist().remove(new ExtendedWhitelistEntry(profile));
            whitelistDataMap.get(profile.getRole()).removeUser(profile);;
        }
    }

    @Override
    public void onInitializeServer() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Bot.stopBot(true), "JDA shutdown"));

        EntryData.register(new EntryData.Team(""));
        EntryData.register(new EntryData.Command("", ""));
        if (FabricLoader.getInstance().isModLoaded("luckperms")) {
            EntryData.register(new LuckpermsEntry.Permission(""));
            EntryData.register(new LuckpermsEntry.Group(""));
        }

        if (!WHITELIST_CACHE_FILE.exists()) {
            try {
                WHITELIST_CACHE.save();
            } catch (IOException e) {
                LOGGER.warn("Failed to save whitelist cache: ", e);
            }
        }

        CONFIG.subscribeToEntries(newEntries -> {
            whitelistDataMap.clear();
            newEntries.forEach(entry -> entry.getRoleIds().forEach(id -> AutoWhitelist.whitelistDataMap.put(id, entry)));
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            AutoWhitelist.server = server;
            CONFIG.load();
            loadWhitelistCache();
            if (!server.isOnlineMode()) {
                LOGGER.warn("Offline server detected!");
                LOGGER.warn("This mod does not offer support for offline servers!");
                LOGGER.warn("Using a whitelist on an offline server offers little to no protection");
                LOGGER.warn("AutoWhitelist may not work properly, fully or at all on an offline server");
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AutoWhitelistCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> Bot.stopBot(false)));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> new Bot().start()));

        Placeholders.register(new Identifier(MOD_ID, "prefix"),
                (ctx, arg) -> PlaceholderResult.value(Text.literal(AutoWhitelist.CONFIG.prefix()))
        );
    }

    public static ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = server.getOverworld();
        return new ServerCommandSource(server, serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO,
                serverWorld, 4, "AutoWhitelist", Text.literal("AutoWhitelist"), server, null);
    }

    public static void loadWhitelistCache() {
        try {
            WHITELIST_CACHE.load();
        } catch (Exception exception) {
            LOGGER.warn("Failed to load whitelist cache: ", exception);
        }
    }

    static {
        CONFIG = Configs.createAndLoad(builder -> {
            builder.registerDeserializer(JsonObject.class, EntryData.class, (jsonObject, m) -> EntryData.deserialize(((JsonPrimitive) jsonObject.get("type")).asString(), jsonObject));
            builder.registerSerializer(EntryData.class, (entryData, marshaller) -> {
                JsonObject json = entryData.serialize();
                json.put("type", new JsonPrimitive(entryData.getType().name()));
                return json;
            });
        });
    }
}
