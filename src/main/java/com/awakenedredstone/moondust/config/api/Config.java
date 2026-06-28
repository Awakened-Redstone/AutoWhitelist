package com.awakenedredstone.moondust.config.api;

import com.awakenedredstone.moondust.config.api.exception.DataFixerException;
import com.awakenedredstone.moondust.config.api.exception.LoadingException;
import com.awakenedredstone.moondust.jankson.JsonGrammar;
import com.awakenedredstone.moondust.jankson.api.DeserializationException;
import com.awakenedredstone.moondust.jankson.element.JsonElement;
import com.awakenedredstone.moondust.util.LoggingUtil;
import com.mojang.serialization.DataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Config<C extends ConfigValues, T extends ConfigSettings<C>> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Moondust Config");
    private final List<Consumer<C>> listeners = new ArrayList<>();
    private final T configSettings;
    private boolean loading = false;
    private C config;

    public Config(T configSettings) {
        this.configSettings = configSettings;
    }

    public void listen(Consumer<C> listener) {
        this.listeners.add(listener);
    }

    public void save() {
        if (loading) return;

        if (config == null) {
            config = configSettings.createDefault();
        }

        save(configSettings.getInterpreter().toJson(config));
    }

    protected void save(JsonElement configJson) {
        var fileName = configSettings.getFileLocation().getFileName().toString();
        try {
            if (!configSettings.getFileLocation().getParent().toFile().exists() && !configSettings.getFileLocation().getParent().toFile().mkdirs()) {
                LOGGER.error("Could not create config path for {}!", fileName);
                return;
            }

            if (configExists()) {
                LOGGER.info("Updating {}, the old version will be available at {}.old", fileName, fileName);
                Files.move(configSettings.getFileLocation(), Paths.get(configSettings.getFileLocation().toAbsolutePath() + ".old"));
            }

            Files.writeString(configSettings.getFileLocation(), configJson.toJson(JsonGrammar.JANKSON_TRUSTED), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not update config for {}!", fileName, e);
        }
    }

    public boolean configExists() {
        return Files.exists(configSettings.getFileLocation());
    }

    /**
     * Load the config represented by this wrapper from
     * its associated file, or create it if it does not exist.
     * <br/>
     * Any error that occurs during the processing will be forwarded under a wrapper and must be caught,
     * this is for methods to know that the loading failed and why.
     *
     * @return If the config was loaded successfully
     */
    public boolean tryLoad() {
        try {
            load();
            return true;
        } catch (LoadingException e) {
            LOGGER.error(LoggingUtil.simpleException("Failed to load config", e), e);
        } catch (DataFixerException e) {
            configSettings.dataFixerLogger.error(LoggingUtil.simpleException("Failed to update config", e), e);
        }

        return false;
    }

    /**
     * Load the config represented by this wrapper from
     * its associated file, or create it if it does not exist.
     * <br/>
     * Any error that occurs during the processing will be forwarded under a wrapper and must be caught,
     * this is for methods to know that the loading failed and why.
     *
     * @throws DataFixerException when the error occurs during the config data fixer step -
     * The config is safe at this stage and this failure does not cause problems
     * @throws LoadingException when the error occurs during the loading process -
     * The config is safe at this stage and this failure does not cause problems
     */
    public void load() throws DataFixerException, LoadingException {
        if (!configExists()) {
            this.save();
            return;
        }

        this.config = read();
    }

    public DataResult<C> tryRead() {
        try {
            return DataResult.success(read());
        } catch (DataFixerException e) {
            return DataResult.error(() -> "Failed to update config! " + e.getMessage());
        } catch (LoadingException e) {
            return DataResult.error(() -> "Failed to load config!" + e.getMessage());
        } catch (Exception e) {
            return DataResult.error(() -> "Unhandled error while loadin the config!" + e.getMessage());
        }
    }

    /**
     * Reads the config represented by this wrapper from
     * its associated file, or create it if it does not exist.
     * <br/>
     * Any error that occurs during the processing will be forwarded under a wrapper and must be caught,
     * this is for methods to know that the loading failed and why.
     *
     * @throws DataFixerException when the error occurs during the config data fixer step -
     * The config is safe at this stage and this failure does not cause problems
     * @throws LoadingException when the error occurs during the loading process -
     * The config is safe at this stage and this failure does not cause problems
     */
    public C read() throws DataFixerException, LoadingException {
        if (!configExists()) {
            throw new IllegalStateException("Could not find a config file to read!");
        }

        this.loading = true;

        ConfigSettings.FixedConfig nextConfig;

        try {
            nextConfig = configSettings.readAndFix();
        } catch (LoadingException e) {
            throw new DataFixerException("Failed to load old config file", e);
        } catch (Exception e) {
            throw new DataFixerException("The config updater crashed", e);
        }

        // Save the updated config
        if (nextConfig.dirty()) {
            save(nextConfig.config());
        }

        try {
            C newConfig = configSettings.getInterpreter().fromJsonCarefully(nextConfig.config(), configSettings.getConfigClass());

            try {
                for (Consumer<C> consumer : listeners) {
                    consumer.accept(newConfig);
                }
            } catch (Exception e) {
                throw new LoadingException("A config listener threw an exception", e);
            }

            return newConfig;
        } catch (DeserializationException e) {
            throw new LoadingException("Failed to deserialize config file", e);
        } catch (Exception e) {
            throw new LoadingException("An error occurred while loading the config file", e);
        } finally {
            this.loading = false;
        }
    }

    public C get() {
        return config;
    }

    /**
     * Serializes the config to a pretty printed string, with all secrets hidden to avoid leaking sensitive data.
     *
     * @return A serialized version of the config, hiding any fields marked as secret
     */
    public String serialize() {
        return configSettings.getInterpreter().toJson(config).toJson(JsonGrammar.JANKSON);
    }
}
