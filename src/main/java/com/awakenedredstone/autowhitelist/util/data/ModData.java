package com.awakenedredstone.autowhitelist.util.data;

import net.fabricmc.loader.api.FabricLoader;

public class ModData {
    public static String getVersion(String id) {
        return FabricLoader.getInstance().getModContainer(id)
          .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
          .orElse("Not present");
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static void ifModLoaded(String id, Runnable then) {
        if (isModLoaded(id)) {
            then.run();
        }
    }
}
