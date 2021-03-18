package com.awakenedredstone.autowhitelist.json;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.GSON;

public class JsonHelper {

    @Nullable
    public static JsonElement parseJsonFile(File file) {
        if (file != null && file.exists() && file.isFile() && file.canRead()) {
            String fileName = file.getAbsolutePath();

            try {
                JsonParser parser = new JsonParser();
                FileReader reader = new FileReader(file);

                JsonElement element = parser.parse(reader);
                reader.close();

                return element;
            } catch (Exception e) {
                AutoWhitelist.LOGGER.error("Failed to parse the JSON file '{}'", fileName, e);
            }
        }

        return null;
    }

    public static boolean writeJsonToFile(JsonObject root, File file) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file);
            writer.write(GSON.toJson(root));
            writer.close();

            return true;
        } catch (IOException e) {
            AutoWhitelist.LOGGER.warn("Failed to write JSON data to file '{}'", file.getAbsolutePath(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                AutoWhitelist.LOGGER.warn("Failed to close JSON file", e);
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
