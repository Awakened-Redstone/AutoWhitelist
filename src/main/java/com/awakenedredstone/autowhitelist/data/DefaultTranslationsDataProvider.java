package com.awakenedredstone.autowhitelist.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DefaultTranslationsDataProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().create();
    private final FabricDataOutput dataOutput;

    public DefaultTranslationsDataProvider(FabricDataOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        try (InputStream inputStream = this.getClass().getResourceAsStream("/data/autowhitelist/lang/en_us.json")) {
            JsonObject json = GSON.fromJson(new String(inputStream.readAllBytes()), JsonObject.class);

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

    @Override
    public String getName() {
        return "Default AutoWhitelist Translation Data Provider";
    }
}
