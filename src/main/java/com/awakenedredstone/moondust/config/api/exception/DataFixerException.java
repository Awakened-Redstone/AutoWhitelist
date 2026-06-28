package com.awakenedredstone.moondust.config.api.exception;

public class DataFixerException extends Exception {
    public DataFixerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataFixerException(Throwable cause) {
        super(cause);
    }
}
