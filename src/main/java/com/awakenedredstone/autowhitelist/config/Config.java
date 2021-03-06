package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;

public class Config {

    private JsonObject config;
    private final File configFile = new File(getConfigDirectory(), "AutoWhitelist.json");

    public File getConfigDirectory() {
        return new File(".", "config");
    }

    public void loadConfigs() {
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            JsonElement element = JsonHelper.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                config = element.getAsJsonObject();
            }
        }
    }

    public JsonObject generateDefaultConfig() {
        JsonObject json = new JsonObject();
        json.add("whitelist-auto-update-delay-seconds", new JsonPrimitive(60L));
        json.add("prefix", new JsonPrimitive("np!"));
        json.add("token", new JsonPrimitive("bot-token"));
        json.add("application-id", new JsonPrimitive("application-id"));
        json.add("discord-server-id", new JsonPrimitive("discord-server-id"));
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
}
