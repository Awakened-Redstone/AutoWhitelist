package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.networking.GeyserProfileRepository;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicReference;

public final class LazyConstants {
    private static final AtomicReference<Boolean> IS_GEYSER_PRESENT = new AtomicReference<>();
    private static final AtomicReference<GeyserProfileRepository> GEYSER_PROFILE_REPOSITORY = new AtomicReference<>();

    public static boolean isUsingGeyser() {
        if (IS_GEYSER_PRESENT.get() == null) {
            IS_GEYSER_PRESENT.set(FabricLoader.getInstance().isModLoaded("geyser-fabric") && FabricLoader.getInstance().isModLoaded("floodgate"));
            if (IS_GEYSER_PRESENT.get()) {
                AutoWhitelist.LOGGER.debug("Geyser and Floodgate detected");
            }
        }

        return IS_GEYSER_PRESENT.get();
    }

    public static GeyserProfileRepository getGeyserProfileRepository() {
        if (GEYSER_PROFILE_REPOSITORY.get() == null) {
            GEYSER_PROFILE_REPOSITORY.set(new GeyserProfileRepository());
        }

        return GEYSER_PROFILE_REPOSITORY.get();
    }
}
