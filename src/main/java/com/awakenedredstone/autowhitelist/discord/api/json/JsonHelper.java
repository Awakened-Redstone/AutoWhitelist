package com.awakenedredstone.autowhitelist.discord.api.json;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static JsonElement parseJsonFile(File file) {
        if (file != null && file.exists() && file.isFile() && file.canRead()) {
            String fileName = file.getAbsolutePath();

            try {
                FileReader reader = new FileReader(file);

                JsonElement element = JsonParser.parseReader(reader);
                reader.close();

                return element;
            } catch (Exception e) {
                LOGGER.error("Failed to parse the JSON file '{}'", fileName, e);
            }
        }

        return null;
    }

    public static boolean writeJsonToFile(JsonObject root, File file) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file);
            writer.write(AutoWhitelist.GSON.toJson(root));
            writer.close();

            return true;
        } catch (IOException e) {
            LOGGER.warn("Failed to write JSON data to file '{}'", file.getAbsolutePath(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to close JSON file", e);
            }
        }

        return false;
    }

    @Nullable
    public static JsonObject getNestedObject(JsonObject parent, String key, boolean create) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            if (!create) {
                return null;
            }

            JsonObject obj = new JsonObject();
            parent.add(key, obj);
            return obj;
        } else {
            return parent.get(key).getAsJsonObject();
        }
    }
}
