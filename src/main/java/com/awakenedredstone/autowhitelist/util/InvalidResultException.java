package com.awakenedredstone.autowhitelist.util;

public class InvalidResultException extends Exception {

    public InvalidResultException() {
        super();
    }

    public InvalidResultException(String message) {
        super(message);
    }

    public InvalidResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidResultException(Throwable cause) {
        super(cause);
    }
}
