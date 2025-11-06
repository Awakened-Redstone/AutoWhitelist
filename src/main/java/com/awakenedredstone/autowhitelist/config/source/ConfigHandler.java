package com.awakenedredstone.autowhitelist.config.source;

import blue.endless.jankson.*;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;
import com.awakenedredstone.autowhitelist.Constants;
import com.awakenedredstone.autowhitelist.config.source.annotation.SkipNameFormat;
import com.awakenedredstone.autowhitelist.config.source.exception.AnnotationParserException;
import com.awakenedredstone.autowhitelist.config.source.exception.DataFixerException;
import com.awakenedredstone.autowhitelist.config.source.exception.LoadingException;
import com.awakenedredstone.autowhitelist.config.source.exception.UpdatingException;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ConfigHandler {
    protected static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist Config");
    protected static final Logger DATA_FIXER_LOGGER = LoggerFactory.getLogger("AutoWhitelist Config Data Fixer");

    private final Map<String, Consumer<Object>> listeners = new HashMap<>();
    private final Map<Integer, Function<JsonObject, JsonObject>> dataFixers;
    private final Jankson interpreter;
    protected final Path fileLocation;
    private final int version;
    protected boolean loading = false;

    @SkipNameFormat
    @Comment("DO NOT CHANGE, MODIFYING THIS VALUE WILL BREAK THE CONFIGURATION FILE")
    public int CONFIG_VERSION;

    public ConfigHandler(String configFile, int version, Jankson interpreter) {
        this.fileLocation = FabricLoader.getInstance().getConfigDir().resolve(configFile + ".json5");
        this.interpreter = interpreter;
        this.version = version;
        this.CONFIG_VERSION = version;
        this.dataFixers = new TreeMap<>(Comparator.naturalOrder());
    }

    @SuppressWarnings("unchecked")
    public <T> void registerListener(String key, Consumer<T> listener) {
        this.listeners.put(key, (Consumer<Object>) listener);
    }

    /**
     * Adds a data fixer for configs on previous versions.
     * The fixer will run for every version between the config version (exclusive) and the latest one (inclusive)
     * @param version The version number of the current config this fixer applies to
     * @param dataFixer The data fixer function, handling the JSON. The json provided in is
     *                  a copy of the latest state, the out is the new JSON to use
     */
    public void registerDataFixer(int version, Function<JsonObject, JsonObject> dataFixer) {
        if (dataFixers.containsKey(version)) {
            throw new IllegalStateException("Can not register multiple data fixers for a version! Tried to register twice for " + version);
        }

        dataFixers.put(version, dataFixer);
    }

    public Path getFileLocation() {
        return fileLocation;
    }

    public void save() {
        save(this.interpreter.toJson(this));
    }

    protected void save(JsonElement config) {
        if (this.loading) return;

        try {
            if (!this.getFileLocation().getParent().toFile().exists() && !this.getFileLocation().getParent().toFile().mkdirs()) {
                LOGGER.error("Could not create config path!");
                return;
            }

            Files.writeString(this.getFileLocation(), config.toJson(Constants.GRAMMAR), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not save config!", e);
        }
    }

    public void loadSafely() {
        try {
            load();
        } catch (Exception ignored) {}
    }

    /**
     * Load the config represented by this wrapper from
     * its associated file, or create it if it does not exist.
     * <br/>
     * Any error that occurs during the processing will be forwarded under a wrapper and must be caught,
     * this is for methods to know that the loading failed and why.
     *
     * @throws LoadingException when the error occurs during the loading process -
     * The config is safe at this stage and this failure does not cause problems
     * @throws DataFixerException when the error occurs during the config data fixer step -
     * The config is safe at this stage and this failure does not cause problems
     * @throws UpdatingException when the error occurs while updating the fields -
     * This is bad and things may break or behave incorrectly, it should never happen
     */
    public void load() throws LoadingException, DataFixerException, UpdatingException {
        if (!configExists()) {
            this.save();
            return;
        }

        this.loading = true;

        JsonObject json;

        try {
            json = configFixerUpper();
        } catch (AnnotationParserException e) {
            LOGGER.error("Config violation! A constraint was violated!", e);
            throw new LoadingException(e);
        } catch (IOException e) {
            LOGGER.error("Failed to read config file!", e);
            throw new LoadingException(e);
        } catch (SyntaxError e) {
            LOGGER.error("Failed to parse config file, invalid JSON!", e);
            throw new LoadingException(e);
        } catch (Exception e) {
            DATA_FIXER_LOGGER.error("The config updater crashed!", e);
            throw new DataFixerException(e);
        }

        try {
            updateConfig(json);
        } catch (DeserializationException e) {
            LOGGER.error("Failed to deserialize config file!", e);
            throw new LoadingException(e);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error("Impossible state while loading config!", e);
            throw new UpdatingException(e);
        } catch (Exception e) {
            LOGGER.error("An error occurred while loading the config file!", e);
            throw new LoadingException(e);
        } finally {
            this.loading = false;
        }
    }

    protected JsonObject configFixerUpper() throws Exception {
        AtomicReference<JsonObject> json = new AtomicReference<>(this.interpreter.load(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8)));

        dataFixers.forEach((fixerVersion, dataFixer) -> {
            if (fixerVersion > json.get().getInt("CONFIG_VERSION", version)) {
                json.getAndUpdate(jsonObject -> dataFixer.apply(jsonObject.clone()));
            }
        });

        return json.get();
    }

    protected void updateConfig(JsonObject json) throws Exception {
        ConfigHandler newValues = this.interpreter.fromJsonCarefully(json, this.getClass());

        // Update fields with new values
        for (var field : this.getClass().getDeclaredFields()) {
            Object newValue = field.get(newValues);
            if (listeners.containsKey(field.getName()) && !Objects.equals(newValue, field.get(this))) {
                try {
                    listeners.get(field.getName()).accept(newValue);
                } catch (Exception e) {
                    LOGGER.error("Failed to call listener while updating config, the mod may not behave as expected!", e);
                }
            }

            field.set(this, newValue);
        }
    }

    public boolean canLoad() {
        if (!configExists()) {
            return false;
        }

        try {
            this.interpreter.fromJsonCarefully(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8), this.getClass());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean configExists() {
        return Files.exists(this.getFileLocation());
    }

    public String toString() {
        return this.interpreter.toJson(this).toJson(JsonGrammar.JANKSON);
    }

    public Jankson getInterpreter() {
        return interpreter;
    }
}
