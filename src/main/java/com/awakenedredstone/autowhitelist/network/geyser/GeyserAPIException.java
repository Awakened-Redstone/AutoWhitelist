package com.awakenedredstone.autowhitelist.network.geyser;

import com.mojang.authlib.exceptions.MinecraftClientException;

public class GeyserAPIException extends MinecraftClientException {
    public GeyserAPIException(ErrorType type, String message) {
        super(type, message);
    }

    public GeyserAPIException(ErrorType type, String message, Throwable cause) {
        super(type, message, cause);
    }
}
