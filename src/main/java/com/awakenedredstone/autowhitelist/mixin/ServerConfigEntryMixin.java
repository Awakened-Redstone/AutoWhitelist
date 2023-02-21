package com.awakenedredstone.autowhitelist.mixin;

import net.minecraft.server.ServerConfigEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerConfigEntry.class)
public interface ServerConfigEntryMixin<T> {
    @Accessor T getKey();
}
