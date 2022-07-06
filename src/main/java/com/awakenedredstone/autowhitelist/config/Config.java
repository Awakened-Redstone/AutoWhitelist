package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.json.JsonHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.text.Text;

import java.io.*;
import java.util.stream.Collectors;

public class Config {

    private ConfigData configData;
    private final File configFile = new File(getConfigDirectory(), "autowhitelist.json");
    private final byte configVersion = 3;

    public File getConfigDirectory() {
        return new File(".", "config/autowhitelist");
    }

    public void loadConfigs() {
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                configData = AutoWhitelist.GSON.fromJson(reader, ConfigData.class);

                if (configData.whitelistScheduledVerificationSeconds <= 0) {
                    configData.whitelistScheduledVerificationSeconds = 30;
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time can't be equals to or lower than 0. It has been set to 30 (not on the file)");
                    try {
                        AutoWhitelist.getCommandSource().sendFeedback(Text.literal("Whitelist scheduled verification time can't be equals to or lower than 0. It has been set to 30 (not on the file)"), true);
                    } catch (NullPointerException ignored) {}
                } else if (configData.whitelistScheduledVerificationSeconds < 3) {
                    configData.whitelistScheduledVerificationSeconds = 30;
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time have to be at least 3 seconds. It has been set to 30 (not on the file)");
                    try {
                        AutoWhitelist.getCommandSource().sendFeedback(Text.literal("Whitelist scheduled verification time have to be at least 3 seconds. It has been set to 30 (not on the file)"), true);
                    } catch (NullPointerException ignored) {}
                } else if (configData.whitelistScheduledVerificationSeconds < 30) {
                    AutoWhitelist.LOGGER.warn("Whitelist scheduled verification time is really low. It is not recommended to have it lower than 30 seconds, since it can affect the server performance.");
                    try {
                        AutoWhitelist.getCommandSource().sendFeedback(Text.literal("Whitelist scheduled verification time is really low. It is not recommended to have it lower than 30 seconds, since it can affect the server performance."), true);
                    } catch (NullPointerException ignored) {}
                }
            } catch (IOException e) {
                AutoWhitelist.LOGGER.error("Failed to load configurations!", e);
            }
        }
    }

    public JsonObject defaultConfig() {
        return AutoWhitelist.GSON.toJsonTree(new ConfigData()).getAsJsonObject();
    }

    public ConfigData getConfigData() {
        return configData;
    }
}

