package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class WeakRegistries {
    public static final Registry<ActionType<?>> ACTION_REGISTRY = new MappedRegistry<>(
      ResourceKey.createRegistryKey(AutoWhitelist.id("entry_action")),
      Lifecycle.stable(),
      false
    );
}
