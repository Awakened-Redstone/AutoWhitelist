package com.awakenedredstone.autowhitelist.config.source;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.Constants;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class ConfigHandler {
    protected static final Logger LOGGER = /*? if >=1.18.2 {*/LoggerFactory/*?} else {*//*LogManager*//*?}*/.getLogger("AutoWhitelist Config");

    private final Map<String, Consumer<Object>> listeners = new HashMap<>();
    private final Jankson interpreter;
    protected final Path fileLocation;
    protected boolean loading = false;

    public ConfigHandler(String configFile, Jankson interpreter) {
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
            Files.writeString(this.getFileLocation(), this.interpreter.toJson(this).toJson(Constants.GRAMMAR), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AutoWhitelist.LOGGER.warn("Could not save config!", e);
        }
    }

    protected void save(JsonObject config) {
        if (this.loading) return;

        try {
            this.getFileLocation().getParent().toFile().mkdirs();
            Files.writeString(this.getFileLocation(), config.toJson(Constants.GRAMMAR), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AutoWhitelist.LOGGER.warn("Could not save config!", e);
        }
    }

    /**
     * Load the config represented by this wrapper from
     * its associated file, or create it if it does not exist
     */
    public void load() {
        if (!configExists()) {
            this.save();
            return;
        }

        try {
            this.loading = true;
            ConfigHandler newValues = this.interpreter.fromJsonCarefully(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8), this.getClass());

            if (newValues == null) {
                LOGGER.error("An unknown error occurred when trying to load the configs!");
                return;
            }

            // Update values with new values
            for (var field : this.getClass().getDeclaredFields()) {
                Object newValue = field.get(newValues);
                if (listeners.containsKey(field.getName()) && !Objects.equals(newValue, field.get(this))) {
                    listeners.get(field.getName()).accept(newValue);
                }

                field.set(this, newValue);
            }
        } catch (AnnotationParserException e) {
            AutoWhitelist.LOGGER.error("Invalid config! Please follow the constraints", e);
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("Could not load config!", e);
            //TODO: Do not continue the mod loading
        } finally {
            this.loading = false;
        }
    }

    public boolean tryLoad() {
        if (!configExists()) {
            return false;
        }

        try {
            this.interpreter.fromJson(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8), this.getClass());
        } catch (IOException | SyntaxError e) {
            return false;
        }
        return true;
    }

    public boolean configExists() {
        return Files.exists(this.getFileLocation());
    }

    public <T> void registerListener(String key, Consumer<T> listener) {
        //noinspection unchecked
        this.listeners.put(key, (Consumer<Object>) listener);
    }

    public String toString() {
        return this.interpreter.toJson(this).toJson(JsonGrammar.JANKSON);
    }

    public Jankson getInterpreter() {
        return interpreter;
    }
}
