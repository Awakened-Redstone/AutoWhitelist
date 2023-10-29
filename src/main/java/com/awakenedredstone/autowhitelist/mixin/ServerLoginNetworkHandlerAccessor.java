package com.awakenedredstone.autowhitelist.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginNetworkHandler.class)
public interface ServerLoginNetworkHandlerAccessor {
    @Accessor GameProfile getProfile();
    @Accessor ClientConnection getConnection();
}
