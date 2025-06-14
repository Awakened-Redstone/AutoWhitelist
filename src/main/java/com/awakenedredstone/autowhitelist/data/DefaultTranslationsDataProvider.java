package com.awakenedredstone.autowhitelist.data;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.google.gson.*;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DefaultTranslationsDataProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().create();
    private final FabricDataOutput dataOutput;

    public DefaultTranslationsDataProvider(FabricDataOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        boolean cat = AutoWhitelist.CONFIG.discordServerId == 1016206797389975612L;
        try (InputStream inputStream = this.getClass().getResourceAsStream("/data/autowhitelist/lang/en_us.json")) {
            JsonObject json = GSON.fromJson(new String(inputStream.readAllBytes()), JsonObject.class);

            if (cat) {
                Map<String, JsonElement> map = json.asMap();
                Set<Map.Entry<String, JsonElement>> entries = new HashSet<>(map.entrySet());
                map.clear();
                for (Map.Entry<String, JsonElement> entry : entries) {
                    if (entry.getValue() instanceof JsonPrimitive primitive && primitive.isString()) {
                        map.put(entry.getKey(), new JsonPrimitive(yipify(primitive.getAsString())));
                    } else {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }
                json.asMap();
            }

            return DataProvider.writeToPath(writer, json, getLangFilePath("en_us"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getLangFilePath(String code) {
        return dataOutput
          .getResolver(DataOutput.OutputType.DATA_PACK, "lang")
          .resolveJson(Identifier.of(dataOutput.getModId(), code));
    }

    public static String yipify(String chatText) {
        if (chatText.isEmpty() || chatText.charAt(0) == '\\') {
            return chatText;
        }

        StringBuilder yipping = new StringBuilder();
        char[] theYip = {'y', 'i', 'p'};
        Set<Character> allowable = Set.of('!', '?', '~', '.', ',', ' ');
        for (int i = 0, yipi = 0; i < chatText.length(); i++, yipi++) {
            char current = chatText.charAt(i);
            if (allowable.contains(current)) {
                yipping.append(current);
                yipi = 2; // effectively 0 after ++
            } else {
                yipping.append(theYip[yipi % theYip.length]);
            }
        }
        return yipping.toString();
    }

    @Override
    public String getName() {
        return "Default Translation Data Provider";
    }
}
