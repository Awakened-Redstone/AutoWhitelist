package com.awakenedredstone.autowhitelist.util;

public class FailedToUpdateWhitelistException extends Exception {

    public FailedToUpdateWhitelistException() {
        super();
    }

    public FailedToUpdateWhitelistException(String message) {
        super(message);
    }

    public FailedToUpdateWhitelistException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToUpdateWhitelistException(Throwable cause) {
        super(cause);
    }
}
