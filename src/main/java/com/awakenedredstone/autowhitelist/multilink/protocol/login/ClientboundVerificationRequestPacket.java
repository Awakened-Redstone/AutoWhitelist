package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundVerificationRequestPacket implements Packet<MLClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundVerificationRequestPacket> STREAM_CODEC = Packet.codec(ClientboundVerificationRequestPacket::write, ClientboundVerificationRequestPacket::new);
    final byte[] challenge;

    public ClientboundVerificationRequestPacket(FriendlyByteBuf buffer) {
        this.challenge = buffer.readByteArray();
    }

    public ClientboundVerificationRequestPacket(byte[] challenge) {
        this.challenge = challenge;
    }

    @Override
    public PacketType<? extends Packet<MLClientLoginPacketListener>> type() {
        return MLLoginPacketTypes.AUTHENTICATE;
    }

    @Override
    public void handle(MLClientLoginPacketListener listener) {
        listener.handleVerificationPing(this);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByteArray(this.challenge);
    }
}
