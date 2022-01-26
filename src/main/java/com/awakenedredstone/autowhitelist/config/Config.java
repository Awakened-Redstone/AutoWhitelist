package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.text.LiteralText;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Config {

    private ConfigData configData;
    private final File configFile = new File(getConfigDirectory(), "AutoWhitelist.json");
    private final float configVersion = 2.1f;

    public File getConfigDirectory() {
        return new File(".", "config");
    }

    public void loadConfigs() {
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                StringReader stringReader = new StringReader(json);

                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                if (jsonObject.get("version") == null || jsonObject.get("version").getAsFloat() != configVersion) {

                    jsonObject.add("owners", new JsonArray());

                    JsonHelper.writeJsonToFile(jsonObject, configFile);
                }

                configData = AutoWhitelist.GSON.fromJson(stringReader, ConfigData.class);

                if (configData.whitelistScheduledVerificationSeconds <= 0) {
                    configData.whitelistScheduledVerificationSeconds = 30;
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time can't be equals to or lower than 0. It has been set to 30 (not on the file)");
                    try {
                        AutoWhitelist.server.getCommandSource().sendFeedback(new LiteralText("Whitelist scheduled verification time can't be equals to or lower than 0. It has been set to 30 (not on the file)"), true);
                    } catch (NullPointerException ignored) {}
                } else if (configData.whitelistScheduledVerificationSeconds < 3) {
                    configData.whitelistScheduledVerificationSeconds = 30;
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time have to be at least 3 seconds. It has been set to 30 (not on the file)");
                    try {
                        AutoWhitelist.server.getCommandSource().sendFeedback(new LiteralText("Whitelist scheduled verification time have to be at least 3 seconds. It has been set to 30 (not on the file)"), true);
                    } catch (NullPointerException ignored) {}
                } else if (configData.whitelistScheduledVerificationSeconds < 30) {
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time is really low. It is not recommended to have it lower than 30 seconds, since it can affect the server performance.");
                    try {
                        AutoWhitelist.server.getCommandSource().sendFeedback(new LiteralText("Whitelist scheduled verification time is really low. It is not recommended to have it lower than 30 seconds, since it can affect the server performance."), true);
                    } catch (NullPointerException ignored) {}
                }
            } catch (IOException e) {
                AutoWhitelist.LOG4J_LOGGER.error(e);
            }
        }
    }

    public JsonObject generateDefaultConfig() {
        JsonObject json = new JsonObject();
        json.add("version", new JsonPrimitive(configVersion));
        json.add("whitelistScheduledVerificationSeconds", new JsonPrimitive(60L));
        json.add("owners", new JsonArray());
        json.add("prefix", new JsonPrimitive("np!"));
        json.add("token", new JsonPrimitive("bot-token"));
        json.add("clientId", new JsonPrimitive("client-id"));
        json.add("discordServerId", new JsonPrimitive("discord-server-id"));
        JsonObject whitelistJson = JsonHelper.getNestedObject(json, "whitelist", true);
        if (whitelistJson == null) {
            AutoWhitelist.LOGGER.error("Something went wrong when generating the default config file!");
            return json;
        }

        {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive("youtube-role-id"));
            array.add(new JsonPrimitive("twitch-role-id"));
            whitelistJson.add("tier2-team-id", array);
        }

        {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive("youtube-role-id"));
            array.add(new JsonPrimitive("twitch-role-id"));
            whitelistJson.add("tier3-team-id", array);
        }
        return json;
    }

    public ConfigData getConfigData() {
        return configData;
    }
}

