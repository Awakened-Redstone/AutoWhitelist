package com.awakenedredstone.autowhitelist.util;

import discord4j.rest.http.client.ClientException;

public class LoggingUtil {
    public static String getErrorResponseMessage(Throwable throwable) {
        return JvmViolations.<Throwable, ClientException>upperCast(throwable)
          .getErrorResponse()
          .map(errorResponse -> "(denied with response " + errorResponse.getFields() + ")")
          .orElse("");
    }
}
