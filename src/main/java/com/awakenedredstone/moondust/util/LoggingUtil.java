package com.awakenedredstone.moondust.util;

import com.awakenedredstone.autowhitelist.util.string.LinedStringBuilder;
import com.awakenedredstone.moondust.jankson.api.SyntaxException;

public class LoggingUtil {
    public static String simpleException(String message, Throwable throwable) {
        var builder = new LinedStringBuilder(message);
        builder.appendLine("Exception stack:");
        builder.appendLine("  ", exceptionMessage(throwable));
        Throwable cause = throwable.getCause();
        while (cause != null) {
            builder.appendLine("  ", exceptionMessage(cause));
            cause = cause.getCause();
        }

        if (throwable.getSuppressed().length > 1) {
            builder.appendLine("Suppressed exceptions:");
            for (Throwable suppressed : throwable.getSuppressed()) {
                builder.appendLine("| ", exceptionMessage(suppressed));
            }
        }

        return builder.toString();
    }

    public static String exceptionMessage(Throwable throwable) {
        if (throwable.getMessage() == null) return throwable.getClass().getName();
        var builder = new LinedStringBuilder(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());

        if (throwable instanceof SyntaxException e) {
            builder.appendLine("    ", e.getLineMessage());
        }

        return builder.toString();
    }
}
