package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.config.Config;
import com.awakenedredstone.autowhitelist.config.ConfigData;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AutoWhitelist implements ModInitializer {

    public static MinecraftServer server;

    public static final Config config = new Config();
    public static final Logger logger = LogManager.getLogger("AutoWhitelist");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File configFile = new File(config.getConfigDirectory(), "auto-whitelist.json");

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    public static void updateWhitelist() {
        logger.info("Updating whitelist.");
        PlayerManager playerManager = server.getPlayerManager();
        Whitelist whitelist = playerManager.getWhitelist();
        List<String> usernames = new SQLite().getUsernames();


        for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
            Scoreboard scoreboard = server.getScoreboard();
            Team team = scoreboard.getTeam(entry.getKey());
            if (team == null) {
                logger.error("Could not update whitelist, got null Team!");
                return;
            }
            List<String> invalidPlayers = team.getPlayerList().stream().filter(player -> {
                try {
                    GameProfile profile = new GameProfile(UUID.fromString(getUUID(player)), player);
                    return !usernames.contains(player) && !playerManager.isOperator(profile);
                } catch (Exception e) {
                    logger.error("Failed to update whitelist!", e);
                    return false;
                }
            }).collect(Collectors.toList());
            invalidPlayers.forEach(player -> scoreboard.removePlayerFromTeam(player, team));
        }

        try {
            for (String username : playerManager.getWhitelistedNames()) {
                GameProfile profile = new GameProfile(UUID.fromString(getUUID(username)), username);
                if (!usernames.contains(username) && !playerManager.isOperator(profile)) {
                    whitelist.remove(new WhitelistEntry(profile));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update whitelist!", e);
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        for (MemberPlayer player : new SQLite().getMembers()) {
            Team team = scoreboard.getTeam(player.getTeam());
            if (team == null) {
                logger.error("Could not update whitelist, got null Team!");
                return;
            }
            if (!whitelist.isAllowed(player.getProfile())) {
                whitelist.add(new WhitelistEntry(player.getProfile()));
            }
            if (scoreboard.getPlayerTeam(player.getProfile().getName()) != team) {
                scoreboard.addPlayerToTeam(player.getProfile().getName(), team);
            }
        }
        logger.info("Whitelist update complete.");
    }

    public static void removePlayer(GameProfile player) {
        if (server.getPlayerManager().getWhitelist().isAllowed(player)) {
            server.getPlayerManager().getWhitelist().remove(new WhitelistEntry(player));
            Scoreboard scoreboard = server.getScoreboard();
            scoreboard.clearPlayerTeam(player.getName());
        }
    }

    private static String getUUID(String username) throws Exception {
        URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username));

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            String _uuid = json.getAsJsonObject().get("id").getAsString();
            if (_uuid.length() != 32) throw new IllegalArgumentException("Invalid UUID string:" + _uuid);
            String[] split = new String[]{_uuid.substring(0, 8), _uuid.substring(8, 12), _uuid.substring(12, 16), _uuid.substring(16, 20), _uuid.substring(20, 32)};
            StringBuilder uuid_ = new StringBuilder(36);
            uuid_.append(split[0]).append("-");
            uuid_.append(split[1]).append("-");
            uuid_.append(split[2]).append("-");
            uuid_.append(split[3]).append("-");
            uuid_.append(split[4]);
            return uuid_.toString();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
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

}
