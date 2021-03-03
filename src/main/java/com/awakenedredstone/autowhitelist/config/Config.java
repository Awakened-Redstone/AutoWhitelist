package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.*;
import java.util.stream.Collectors;

public class Config {

    private JsonObject config;
    private ConfigData configData;
    private final File configFile = new File(getConfigDirectory(), "auto-whitelist.json");

    public File getConfigDirectory() {
        return new File(".", "config");
    }

    public void loadConfigs() {
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                StringReader stringReader = new StringReader(json);
                configData = AutoWhitelist.GSON.fromJson(stringReader, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public JsonObject generateDefaultConfig() {
        JsonObject json = new JsonObject();
        json.add("whitelistAutoUpdateDelaySeconds", new JsonPrimitive(60L));
        json.add("prefix", new JsonPrimitive("np!"));
        json.add("token", new JsonPrimitive("discord-token"));
        json.add("applicationId", new JsonPrimitive("application-id"));
        json.add("discordServerId", new JsonPrimitive("discord-server-id"));
        JsonObject whitelistJson = JsonHelper.getNestedObject(json, "whitelist", true);
        if (whitelistJson == null) {
            AutoWhitelist.logger.error("Something went wrong when generating the default config file!");
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

    public JsonObject getConfigs() {
        return config;
    }

    public ConfigData getConfigData() {
        return configData;
    }
}
