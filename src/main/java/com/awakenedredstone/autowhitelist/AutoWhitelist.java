package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.config.Config;
import com.awakenedredstone.autowhitelist.config.ConfigData;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.awakenedredstone.autowhitelist.lang.JigsawLanguage;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.server.AutoWhitelistServer;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.InvalidTeamNameException;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.lang.JigsawLanguage.translations;

public class AutoWhitelist implements ModInitializer {
    public static MinecraftServer server;

    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File configFile = new File(config.getConfigDirectory(), "AutoWhitelist.json");

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
                    continue;
                }
            } catch (ClassCastException ignored) {
                whitelist.remove(profile);
                scoreboard.clearPlayerTeam(profile.getName());
                continue;
            }

            if (!profile.getName().equals(profile1.getName())) {
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
                scoreboard.clearPlayerTeam(player.getName());
                scoreboard.addPlayerToTeam(player.getName(), team);
            }
        }

        server.kickNonWhitelistedPlayers(server.getCommandSource());
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
        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.generateDefaultConfig(), configFile);
            }
        }
        config.loadConfigs();
    }

    public static void reloadTranslations() {
        try {
            translations.clear();
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
    }

}
