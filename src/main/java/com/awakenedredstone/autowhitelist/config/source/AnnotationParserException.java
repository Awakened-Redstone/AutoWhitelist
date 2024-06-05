package com.awakenedredstone.autowhitelist.config.source;

public class AnnotationParserException extends RuntimeException {
    private final Throwable original;

    public AnnotationParserException(Throwable original) {
        this.original = original;
    }

    public Throwable getOriginal() {
        return original;
    }
}
