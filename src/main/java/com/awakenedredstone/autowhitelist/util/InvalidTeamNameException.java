package com.awakenedredstone.autowhitelist.util;

public class InvalidTeamNameException extends Exception {

    public InvalidTeamNameException() {
        super();
    }

    public InvalidTeamNameException(String message) {
        super(message);
    }

    public InvalidTeamNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTeamNameException(Throwable cause) {
        super(cause);
    }
}
