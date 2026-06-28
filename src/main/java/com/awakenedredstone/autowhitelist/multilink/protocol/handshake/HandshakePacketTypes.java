package com.awakenedredstone.autowhitelist.multilink.protocol.handshake;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;

public class HandshakePacketTypes {
    public static final PacketType<MultilinkHandshakePacket> HANDSHAKE = createServerbound("handshake");

    private static <T extends Packet<ServerHandshakePacketListener>> PacketType<T> createServerbound(String name) {
        return new PacketType<>(PacketFlow.SERVERBOUND, AutoWhitelist.id(name));
    }
}
