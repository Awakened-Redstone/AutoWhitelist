package com.awakenedredstone.autowhitelist.multilink.mixin;

import com.awakenedredstone.autowhitelist.multilink.protocol.handshake.HandshakePacketTypes;
import com.awakenedredstone.autowhitelist.multilink.protocol.handshake.MultilinkHandshakePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandshakeProtocols.class)
public class HandshakeProtocolsMixin {
    @Inject(at = @At(value = "TAIL"), method = /*? if <26.1 {*/ /*"method_56009"*/ /*?} else {*/ "lambda$static$0" /*?}*/)
    private static void injectMultilinkHandshake(ProtocolInfoBuilder<ServerHandshakePacketListener, FriendlyByteBuf, Unit> builder, CallbackInfo ci) {
        builder.addPacket(HandshakePacketTypes.HANDSHAKE, MultilinkHandshakePacket.STREAM_CODEC);
    }
}
