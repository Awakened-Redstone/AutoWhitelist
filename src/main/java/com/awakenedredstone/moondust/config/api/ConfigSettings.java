package com.awakenedredstone.moondust.config.api;

import com.awakenedredstone.moondust.config.api.exception.LoadingException;
import com.awakenedredstone.moondust.jankson.Jankson;
import com.awakenedredstone.moondust.jankson.api.SyntaxException;
import com.awakenedredstone.moondust.jankson.element.JsonObject;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonInteger;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class ConfigSettings<T extends ConfigValues> {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final Logger dataFixerLogger = LoggerFactory.getLogger(this.getClass().getSimpleName() + " Data Fixer");

    private final String fileName;
    private final Path fileLocation;
    private final Jankson interpreter;
    private final int version;
    private final Map<Integer, Function<JsonObject, JsonObject>> dataFixers;
    private final Class<T> configClass;

    public ConfigSettings(String fileName, Class<T> configClass, int version, Jankson interpreter) {
        this.fileName = fileName;
        this.fileLocation = FabricLoader.getInstance().getConfigDir().resolve(fileName + ".json5");
        this.interpreter = interpreter;
        this.version = version;
        this.dataFixers = new TreeMap<>(Comparator.naturalOrder());
        this.configClass = configClass;
    }

    /**
     * Adds a data fixer for configs on previous versions.
     * The fixer will run for every version between the config version (exclusive) and the latest one (inclusive)
     * @param version The version number of the current config this fixer applies to
     * @param dataFixer The data fixer function, handling the JSON. The json provided in is
     *                  a copy of the latest state, the out is the new JSON to use
     */
    public void addDataFixer(int version, Function<JsonObject, JsonObject> dataFixer) {
        if (dataFixers.containsKey(version)) {
            throw new IllegalStateException("Can not register multiple data fixers for a version! Tried to register twice for " + version);
        }

        dataFixers.put(version, dataFixer);
    }

    public Path getFileLocation() {
        return fileLocation;
    }

    FixedConfig readAndFix() throws Exception {
        AtomicReference<JsonObject> json = new AtomicReference<>();
        try {
            json.set(this.interpreter.load(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new LoadingException("Failed to read config file", e);
        } catch (SyntaxException e) {
            throw new LoadingException("Failed to parse config file, invalid JSON", e);
        }

        AtomicBoolean updated = new AtomicBoolean(false);

        for (Map.Entry<Integer, Function<JsonObject, JsonObject>> entry : dataFixers.entrySet()) {
            Integer fixerVersion = entry.getKey();
            Function<JsonObject, JsonObject> dataFixer = entry.getValue();

            int configVersion = json.get().getInt("CONFIG_VERSION", -1);
            if (configVersion == -1) throw new AssertionError("The config does not contain a version");

            if (fixerVersion > configVersion) {
                json.getAndUpdate(jsonObject -> dataFixer.apply(jsonObject.clone()));
                json.get().put("CONFIG_VERSION", new JsonInteger(fixerVersion));
                updated.set(true);
            }
        }

        if (version > json.get().getInt("CONFIG_VERSION", version)) {
            throw new AssertionError("Missing data fixer for version " + version);
        }

        return new FixedConfig(json.get(), updated.get());
    }

    public Jankson getInterpreter() {
        return interpreter;
    }

    public String getFileName() {
        return fileName;
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public T createDefault() {
        try {
            Constructor<T> constructor = configClass.getConstructor();

            T instance = constructor.newInstance();
            instance.CONFIG_VERSION = version;
            return instance;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Config offers no no-arg constructor!");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to initialize default config class", e);
        } catch (InstantiationException e) {
            throw new AssertionError("Config is an abstract class!");
        } catch (IllegalAccessException e) {
            throw new AssertionError("Config offers no accessible no-arg constructor!");
        }
    }

    public record FixedConfig(JsonObject config, boolean dirty) {}
}
