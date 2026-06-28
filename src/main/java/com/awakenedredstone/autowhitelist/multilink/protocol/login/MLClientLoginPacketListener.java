package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.login.*;
import net.minecraft.util.Crypt;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

@NullMarked
public class MLClientLoginPacketListener implements ClientLoginPacketListener, TickablePacketListener {
    public static final Logger LOGGER = LoggerFactory.getLogger(MLClientLoginPacketListener.class);
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private final byte[] secret;
    private final Connection connection;
    private volatile State state = new State();
    private int tick;

    public MLClientLoginPacketListener(Connection connection) {
        this.connection = connection;
        this.secret = AutoWhitelist.config().multilink().secret();
    }

    @Override
    public void tick() {
        // TODO
    }

    @Override
    public void handleHello(ClientboundHelloPacket packet) {
        state.check(States.CONNECTING, "Unexpected encryption packet");
        state.complete();
        state.next();

        try {
            SecretKey secretKey = Crypt.generateSecretKey();
            PublicKey publicKey = packet.getPublicKey();
            Cipher cipher = Crypt.getCipher(2, secretKey);
            Cipher cipher2 = Crypt.getCipher(1, secretKey);
            byte[] challenge = packet.getChallenge();
            ServerboundKeyPacket serverboundKeyPacket = new ServerboundKeyPacket(secretKey, publicKey, challenge);

            this.setEncryption(serverboundKeyPacket, cipher, cipher2);
        } catch (Exception e) {
            throw new IllegalStateException("Protocol error", e);
        }
    }

    private void setEncryption(ServerboundKeyPacket keyPacket, Cipher decryptingCypher, Cipher encryptingCypher) {
        this.connection.send(keyPacket, PacketSendListener.thenRun(() -> {
            this.connection.setEncryptionKey(decryptingCypher, encryptingCypher);
            state.complete();
        }));
    }

    public void handleVerificationPing(ClientboundVerificationRequestPacket packet) {
        state.check(States.ENCRYPTING, "Unexpected verification request");
        state.next();

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

            messageDigest.update(packet.challenge);
            messageDigest.update(secret);

            this.connection.send(new ServerboundVerificationChallengePacket(messageDigest.digest()), PacketSendListener.thenRun(() -> state.complete()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to get hashing algorithm");
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        // TODO
    }

    @Override
    public void handleDisconnect(ClientboundLoginDisconnectPacket packet) {
        // TODO
    }

    @Override
    public void handleLoginFinished(ClientboundLoginFinishedPacket packet) {
        throw new UnsupportedOperationException("The login finished packet is not supported on Multilink login");
    }

    @Override
    public void handleCompression(ClientboundLoginCompressionPacket packet) {
        throw new UnsupportedOperationException("Compression is currently not supported in multilink connections");
    }

    @Override
    public void handleCustomQuery(ClientboundCustomQueryPacket packet) {
        throw new UnsupportedOperationException("The custom query packet is not supported on Multilink login");
    }

    @Override
    public void handleRequestCookie(ClientboundCookieRequestPacket packet) {
        throw new UnsupportedOperationException("The cookie request packet is not supported on Multilink login");
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    private static class State {
        private static final int LAST_STATE = States.values().length - 1;
        private static final States[] STATES = States.values();
        private States currentState = STATES[0];
        private boolean complete = false;

        public void next() {
            if (!complete) throw new IllegalStateException("Current state is not complete");
            int currentIndex = currentState.ordinal();
            if (currentIndex == LAST_STATE) throw new IllegalStateException("Already at the last state");
            currentState = STATES[currentIndex + 1];
            complete = false;
        }

        public void complete() {
            if (complete) throw new IllegalStateException("State is already complete");
            complete = true;
        }

        public void check(States expect, final String message, final Object... values) {
            if (currentState != expect) {
                var main = new IllegalStateException("Expected to be " + expect + " but is " + currentState);
                var exception = new IllegalStateException(getMessage(message, values), main);
                main.setStackTrace(new StackTraceElement[0]); // Remove stacktrace, leave it to the main one to show it.
                throw exception;
            }
        }

        private static String getMessage(final String message, final Object... values) {
            return ArrayUtils.isEmpty(values) ? message : String.format(message, values);
        }
    }

    private enum States {
        CONNECTING,
        ENCRYPTING,
        AUTHENTICATING,
        VALIDATING,
        ACCEPTED
    }
}
