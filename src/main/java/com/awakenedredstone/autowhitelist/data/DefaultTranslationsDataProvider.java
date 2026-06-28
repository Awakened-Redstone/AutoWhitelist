package com.awakenedredstone.autowhitelist.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DefaultTranslationsDataProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().create();
    private final FabricPackOutput dataOutput;

    public DefaultTranslationsDataProvider(FabricPackOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput writer) {
        try (InputStream inputStream = this.getClass().getResourceAsStream("/data/autowhitelist/lang/en_us.json")) {
            JsonObject json = GSON.fromJson(new String(inputStream.readAllBytes()), JsonObject.class);

            return DataProvider.saveStable(writer, json, getLangFilePath("en_us"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getLangFilePath(String code) {
        return dataOutput
          .createPathProvider(PackOutput.Target.DATA_PACK, "lang")
          .json(Identifier.fromNamespaceAndPath(dataOutput.getModId(), code));
    }

    @Override
    public @NotNull String getName() {
        return "Default AutoWhitelist Translation Data Provider";
    }
}
