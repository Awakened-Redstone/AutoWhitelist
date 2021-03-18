package com.awakenedredstone.autowhitelist.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.GSON;

public class UrlQuery {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static Map<String, String> getMojangStatus() throws IOException {
        URL url = new URL("https://status.mojang.com/check");

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            JsonArray statusArray = json.getAsJsonArray();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return GSON.fromJson(statusArray.toString(), type);
        }
    }

    public static String statusOf(String url) throws IOException {
        return getMojangStatus().get(url);
    }
}
