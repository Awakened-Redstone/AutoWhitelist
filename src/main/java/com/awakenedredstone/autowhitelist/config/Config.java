package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Config {
    private final Map<String, Consumer<Object>> listeners = new HashMap<>();
    private final Jankson interpreter;
    protected final Path fileLocation;
    protected boolean loading = false;

    public Config(String configFile, Jankson interpreter) {
        this.fileLocation = FabricLoader.getInstance().getConfigDir().resolve(configFile + ".json5");
        this.interpreter = interpreter;
    }

    public Path getFileLocation() {
        return fileLocation;
    }

    public void save() {
        if (this.loading) return;

        try {
            this.getFileLocation().getParent().toFile().mkdirs();
            Files.writeString(this.getFileLocation(), this.interpreter.toJson(this).toJson(JsonGrammar.JANKSON), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AutoWhitelist.LOGGER.warn("Could not save config!", e);
        }
    }

    /**
     * Load the config represented by this wrapper from
     * its associated file, or create it if it does not exist
     */
    @SuppressWarnings({"unchecked"})
    public void load() {
        if (!Files.exists(this.getFileLocation())) {
            this.save();
            return;
        }

        try {
            this.loading = true;
            Config newValues = this.interpreter.fromJson(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8), this.getClass());

            //Update values with new values
            for (var field : this.getClass().getDeclaredFields()) {
                Object newValue = field.get(newValues);
                if (listeners.containsKey(field.getName()) && !Objects.equals(newValue, field.get(this))) {
                    listeners.get(field.getName()).accept(newValue);
                }

                field.set(this, newValue);
            }

        } catch (IOException | SyntaxError | IllegalAccessException e) {
            AutoWhitelist.LOGGER.warn("Could not load config!", e);
        } finally {
            this.loading = false;
        }
    }

    public <T> void registerListener(String key, Consumer<T> listener) {
        this.listeners.put(key, (Consumer<Object>) listener);
    }
}
