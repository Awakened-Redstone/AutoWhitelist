package com.awakenedredstone.autowhitelist.multilink.protocol.login;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.google.common.primitives.Ints;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;

@NullMarked
public class MLServerLoginPacketListener implements ServerLoginPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLServerLoginPacketListener.class);
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private final byte[] challenge;
    private final MinecraftServer server;
    private final Connection connection;
    private volatile State state = new State();
    private int tick;

    public MLServerLoginPacketListener(MinecraftServer server, Connection connection) {
        this.server = server;
        this.connection = connection;
        this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
    }

    @Override
    public void tick() {
        if (this.tick++ == MAX_TICKS_BEFORE_LOGIN) {
            this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
        }

        // TODO
    }

    @Override
    public void handleKey(ServerboundKeyPacket packet) {
        state.check(States.KEY, "Unexpected key packet");
        state.complete();
        state.next();

        try {
            PrivateKey privateKey = this.server.getKeyPair().getPrivate();
            if (!packet.isChallengeValid(this.challenge, privateKey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packet.getSecretKey(privateKey);
            Cipher cipher = Crypt.getCipher(2, secretKey);
            Cipher cipher2 = Crypt.getCipher(1, secretKey);
            this.connection.setEncryptionKey(cipher, cipher2);
            state.complete();
        } catch (CryptException e) {
            throw new IllegalStateException("Protocol error", e);
        }
    }

    public void handleVerificationChallenge(ServerboundVerificationChallengePacket packet) {
        state.check(States.ENCRYPTING, "Unexpected challenge response");
        state.next();
        try {
            if (!packet.isChallengeValid(this.challenge, AutoWhitelist.config().multilink().secret())) {
                this.disconnect(Component.literal("Failed verification challenge"));
                return;
            }

            state.complete();
        } catch (CryptException e) {
            throw new IllegalStateException("Protocol error", e);
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        LOGGER.info("{} failed multilink login: {}", this.getDisplayName(), details.reason().getString());
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public String getDisplayName() {
        return this.connection.getLoggableAddress(this.server.logIPs());
    }

    public void disconnect(Component reason) {
        try {
            LOGGER.debug("(ML) Disconnecting {}: {}", this.getDisplayName(), reason.getString());
            this.connection.send(new ClientboundLoginDisconnectPacket(reason));
            this.connection.disconnect(reason);
        } catch (Exception e) {
            LOGGER.error("Error whilst disconnecting multilink client", e);
        }
    }

    @Override
    public void handleHello(ServerboundHelloPacket packet) {
        throw new UnsupportedOperationException("The hello packet is not supported on Multilink login");
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet) {
        throw new UnsupportedOperationException("The custom query packet is not supported on Multilink login");
    }

    @Override
    public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket packet) {
        throw new UnsupportedOperationException("The login acknowledgement packet is not supported on Multilink login");
    }

    @Override
    public void handleCookieResponse(ServerboundCookieResponsePacket packet) {
        throw new UnsupportedOperationException("The cookie response packet is not supported on Multilink login");
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
        KEY,
        ENCRYPTING,
        VERIFYING,
        WAITING_FOR_DUPE_DISCONNECT,
        ACCEPTED
    }
}
