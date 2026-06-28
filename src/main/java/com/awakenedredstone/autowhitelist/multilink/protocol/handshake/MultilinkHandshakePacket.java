package com.awakenedredstone.autowhitelist.multilink.protocol.handshake;

import com.awakenedredstone.autowhitelist.multilink.mixin.accessor.ServerHandshakePacketListenerImplAccessor;
import com.awakenedredstone.autowhitelist.multilink.protocol.login.MLLoginProtocols;
import com.awakenedredstone.autowhitelist.multilink.protocol.login.MLServerLoginPacketListener;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;

public class MultilinkHandshakePacket implements Packet<ServerHandshakePacketListener> {
    public static final MultilinkHandshakePacket INSTANCE = new MultilinkHandshakePacket();
    public static final StreamCodec<ByteBuf, MultilinkHandshakePacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private MultilinkHandshakePacket() {}

    @Override
    public PacketType<? extends Packet<ServerHandshakePacketListener>> type() {
        return HandshakePacketTypes.HANDSHAKE;
    }

    @Override
    public void handle(ServerHandshakePacketListener handler) {
        if (!(handler instanceof ServerHandshakePacketListenerImplAccessor accessor)) {
            // This should never happen.
            // If a mod adds a new connection, this packet should never be received.
            // If the listener is being replaced on connection setup, I can't do much, a duck would face the same issue.
            // Fully replacing the handshake listener isn't something I would recommend, as it can cause issues with clients.
            throw new IllegalStateException("Packet handler isn't ServerHandshakePacketListenerImpl, likely mod incompatibility");
        }

        Connection connection = accessor.getConnection();
        connection.setupInboundProtocol(MLLoginProtocols.SERVERBOUND, new MLServerLoginPacketListener(accessor.getServer(), connection));
    }
}
