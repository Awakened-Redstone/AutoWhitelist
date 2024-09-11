package com.awakenedredstone.autowhitelist.mixin;

import com.mojang.authlib.GameProfileRepository;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UserCache.class)
public interface UserCacheAccessor {
    @Accessor GameProfileRepository getProfileRepository();
}
