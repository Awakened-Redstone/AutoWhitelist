package com.awakenedredstone.moondust.config.api.exception;

public class LoadingException extends Exception {
    public LoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadingException(Throwable cause) {
        super(cause);
    }
}
