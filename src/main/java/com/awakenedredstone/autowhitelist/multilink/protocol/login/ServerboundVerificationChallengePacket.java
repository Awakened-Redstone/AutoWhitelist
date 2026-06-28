package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.CryptException;
import org.jspecify.annotations.NullMarked;

import java.security.MessageDigest;
import java.util.Arrays;

@NullMarked
public class ServerboundVerificationChallengePacket implements Packet<MLServerLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundVerificationChallengePacket> STREAM_CODEC = Packet.codec(ServerboundVerificationChallengePacket::write, ServerboundVerificationChallengePacket::new);
    private final byte[] token;

    public ServerboundVerificationChallengePacket(FriendlyByteBuf buffer) {
        this.token = buffer.readByteArray();
    }

    public ServerboundVerificationChallengePacket(byte[] token) {
        this.token = token;
    }

    @Override
    public PacketType<? extends Packet<MLServerLoginPacketListener>> type() {
        return MLLoginPacketTypes.SECRET;
    }

    @Override
    public void handle(MLServerLoginPacketListener handler) {
        handler.handleVerificationChallenge(this);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByteArray(this.token);
    }

    boolean isChallengeValid(byte[] salt, byte[] secret) throws CryptException {
        return Arrays.equals(digestData(salt, secret), token);
    }

    private static byte[] digestData(byte[]... data) throws CryptException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

            for (byte[] bs : data) {
                messageDigest.update(bs);
            }

            return messageDigest.digest();
        } catch (Exception e) {
            throw new CryptException(e);
        }
    }
}
