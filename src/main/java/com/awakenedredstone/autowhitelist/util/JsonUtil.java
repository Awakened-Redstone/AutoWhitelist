package com.awakenedredstone.autowhitelist.util;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;

public class JsonUtil {
    public static void rename(JsonObject json, String oldName, String newName) {
        JsonElement element = json.get(oldName);
        if (element != null) {
            json.put(newName, element);
            json.remove(oldName);
        }
    }

    public static void move(JsonObject originalJson, JsonObject newJson, String name) {
        if (originalJson.containsKey(name)) {
            //noinspection DataFlowIssue
            newJson.put(name, originalJson.get(JsonElement.class, name));
            originalJson.remove(name);
        }
    }

    public static void moveAndRename(JsonObject originalJson, JsonObject newJson, String name, String newName) {
        if (originalJson.containsKey(name)) {
            //noinspection DataFlowIssue
            newJson.put(newName, originalJson.get(JsonElement.class, name));
            originalJson.remove(name);
        }
    }
}
