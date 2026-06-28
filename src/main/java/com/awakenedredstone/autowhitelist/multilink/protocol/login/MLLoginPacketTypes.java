package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;

public class MLLoginPacketTypes {
    public static final PacketType<ServerboundKeyPacket> ENCRYPT_RESPONSE = createServerbound("encrypt_response");
    public static final PacketType<ServerboundVerificationChallengePacket> SECRET = createServerbound("secret");

    public static final PacketType<ClientboundHelloPacket> ENCRYPT = createClientbound("encrypt");
    public static final PacketType<ClientboundVerificationRequestPacket> AUTHENTICATE = createClientbound("authenticate");

    private static <T extends Packet<? extends ServerLoginPacketListener>> PacketType<T> createServerbound(String name) {
        return new PacketType<>(PacketFlow.SERVERBOUND, AutoWhitelist.id(name));
    }

    private static <T extends Packet<? extends ClientLoginPacketListener>> PacketType<T> createClientbound(String name) {
        return new PacketType<>(PacketFlow.CLIENTBOUND, AutoWhitelist.id(name));
    }
}
