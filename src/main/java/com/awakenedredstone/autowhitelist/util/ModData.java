package com.awakenedredstone.autowhitelist.util;

import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicReference;

public class ModData {

    public static String getVersion(String modid) {
        AtomicReference<String> version = new AtomicReference<>("");
        FabricLoader.getInstance().getModContainer(modid).ifPresentOrElse(modContainer -> {
            version.set(modContainer.getMetadata().getVersion().getFriendlyString());
        }, () -> {
            version.set("Not present");
        });

        return version.get();
    }

    public static boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
