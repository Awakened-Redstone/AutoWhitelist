package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;


public class MLLoginProtocols {
    public static final SimpleUnboundProtocol<MLServerLoginPacketListener, FriendlyByteBuf> SERVERBOUND_TEMPLATE = ProtocolInfoBuilder.serverboundProtocol(
      ConnectionProtocol.LOGIN, builder -> builder
          .addPacket(MLLoginPacketTypes.ENCRYPT_RESPONSE, ServerboundKeyPacket.STREAM_CODEC)
          .addPacket(MLLoginPacketTypes.SECRET, ServerboundVerificationChallengePacket.STREAM_CODEC)
    );

    public static final ProtocolInfo<MLServerLoginPacketListener> SERVERBOUND = SERVERBOUND_TEMPLATE.bind(FriendlyByteBuf::new);

    public static final SimpleUnboundProtocol<MLClientLoginPacketListener, FriendlyByteBuf> CLIENTBOUND_TEMPLATE = ProtocolInfoBuilder.clientboundProtocol(
      ConnectionProtocol.LOGIN, builder -> builder
          .addPacket(MLLoginPacketTypes.ENCRYPT, ClientboundHelloPacket.STREAM_CODEC)
          .addPacket(MLLoginPacketTypes.AUTHENTICATE, ClientboundVerificationRequestPacket.STREAM_CODEC)
    );

    public static final ProtocolInfo<MLClientLoginPacketListener> CLIENTBOUND = CLIENTBOUND_TEMPLATE.bind(FriendlyByteBuf::new);
}
