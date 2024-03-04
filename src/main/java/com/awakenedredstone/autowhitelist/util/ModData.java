package com.awakenedredstone.autowhitelist.util;

import net.fabricmc.loader.api.FabricLoader;

public class ModData {

    public static String getVersion(String modid) {
        return FabricLoader.getInstance().getModContainer(modid)
          .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
          .orElse("Not present");
    }

    public static boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
