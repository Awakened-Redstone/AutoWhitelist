package com.awakenedredstone.autowhitelist.multilink.mixin.accessor;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerHandshakePacketListenerImpl.class)
public interface ServerHandshakePacketListenerImplAccessor {
    @Accessor Connection getConnection();
    @Accessor MinecraftServer getServer();
}
