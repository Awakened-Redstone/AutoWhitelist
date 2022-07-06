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
}
