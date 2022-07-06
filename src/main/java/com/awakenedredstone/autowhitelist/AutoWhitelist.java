package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.config.Config;
import com.awakenedredstone.autowhitelist.config.ConfigData;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.awakenedredstone.autowhitelist.lang.CustomLanguage;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.server.AutoWhitelistServer;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.InvalidTeamNameException;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.awakenedredstone.autowhitelist.lang.CustomLanguage.translations;

public class AutoWhitelist implements ModInitializer {
    public static MinecraftServer server;

    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File configFile = new File(config.getConfigDirectory(), "autowhitelist.json");

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    public static void updateWhitelist() {
        PlayerManager playerManager = server.getPlayerManager();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) playerManager.getWhitelist();
        Scoreboard scoreboard = server.getScoreboard();

        Collection<? extends WhitelistEntry> entries = whitelist.getEntries();

        List<GameProfile> profiles = entries.stream().map(v -> {
            ((ServerConfigEntryMixin<?>) v).callGetKey();
            return (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey();
        }).toList();

        for (GameProfile profile : profiles) {
            GameProfile profile1 = server.getUserCache().getByUuid(profile.getId()).orElse(null);
            try {
                if (profile1 == null) {
                    removePlayer((ExtendedGameProfile) profile);
                    getCommandSource().sendFeedback(Text.literal("Removing bad entry from " + profile.getName()), true);
                    continue;
                }
            } catch (ClassCastException ignored) {
                getCommandSource().sendFeedback(Text.literal("Removing bad entry from " + profile.getName()), true);
                whitelist.remove(profile);
                scoreboard.clearPlayerTeam(profile.getName());
                continue;
            }

            if (!profile.getName().equals(profile1.getName())) {
                getCommandSource().sendFeedback(Text.literal("Fixing bad entry from " + profile.getName()), true);
                try {
                    ExtendedGameProfile isExtended = (ExtendedGameProfile) profile;
                    whitelist.remove(isExtended);
                    whitelist.add(new ExtendedWhitelistEntry(new ExtendedGameProfile(profile1.getId(), profile1.getName(), isExtended.getTeam(), isExtended.getDiscordId())));
                } catch (ClassCastException ignored) {
                    whitelist.remove(profile);
                    whitelist.add(new WhitelistEntry(profile1));
                }
            }
        }

        List<ExtendedGameProfile> extendedProfiles = profiles.stream().map(v -> {
            try {
                return (ExtendedGameProfile) v;
            } catch (ClassCastException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();

        getConfigData().whitelist.keySet().forEach(teamName -> {
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                LOGGER.error("Could not check for invalid players on team \"{}\", got \"null\" when trying to get \"net.minecraft.scoreboard.Team\" from \"{}\"", teamName, teamName, new InvalidTeamNameException("Tried to get \"net.minecraft.scoreboard.Team\" from \"" + teamName + "\" but got \"null\"."));
                return;
            }
            List<String> invalidPlayers = team.getPlayerList().stream().filter(player -> {
                GameProfile profile = profiles.stream().filter(v -> v.getName().equals(player)).findFirst().orElse(null);
                if (profile == null) return true;
                return !whitelist.isAllowed(profile);
            }).toList();
            invalidPlayers.forEach(player -> scoreboard.removePlayerFromTeam(player, team));
        });

        for (ExtendedGameProfile player : extendedProfiles) {
            if (player == null) continue;
            Team team = scoreboard.getTeam(player.getTeam());

            if (team == null) {
                LOGGER.error("Could not check team information of \"{}\", got \"null\" when trying to get \"net.minecraft.scoreboard.Team\" from \"{}\"", player.getName(), player.getTeam(), new InvalidTeamNameException("Tried to get \"net.minecraft.scoreboard.Team\" from \"" + player.getTeam() + "\" but got \"null\"."));
                return;
            }

            if (scoreboard.getPlayerTeam(player.getName()) != team) {
                scoreboard.addPlayerToTeam(player.getName(), team);
            }
        }

        if (server.getPlayerManager().isWhitelistEnabled()) {
            server.kickNonWhitelistedPlayers(server.getCommandSource());
        }
    }

    public static void removePlayer(ExtendedGameProfile player) {
        if (server.getPlayerManager().getWhitelist().isAllowed(player)) {
            server.getPlayerManager().getWhitelist().remove(new ExtendedWhitelistEntry(player));
            Scoreboard scoreboard = server.getScoreboard();
            scoreboard.clearPlayerTeam(player.getName());
        }
    }

    @Override
    public void onInitialize() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Bot.stopBot(true), "JDA shutdown"));

        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.defaultConfig(), configFile);
            }
        }
        config.loadConfigs();
    }

    public static void reloadTranslations() {
        try {
            {
                InputStream inputStream = AutoWhitelistServer.class.getResource("/messages.json").openStream();
                CustomLanguage.load(inputStream, translations::put);
            }
            File file = new File(config.getConfigDirectory(), "messages.json");
            if (!file.exists()) {
                Files.copy(AutoWhitelistServer.class.getResource("/messages.json").openStream(), file.toPath());
            }

            InputStream inputStream = Files.newInputStream(file.toPath());
            CustomLanguage.load(inputStream, translations::put);
        } catch (Exception e) {
            LOGGER.error("Failed to load translations", e);
        }
    }

    public static ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = server.getOverworld();
        return new ServerCommandSource(server, serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO,
                serverWorld, 4, "AutoWhitelist", Text.literal("AutoWhitelist"), server, null);
    }

}
