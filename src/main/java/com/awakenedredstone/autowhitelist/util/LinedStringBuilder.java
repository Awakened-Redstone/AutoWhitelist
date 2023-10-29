package com.awakenedredstone.autowhitelist.util;

public class LinedStringBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();

    public LinedStringBuilder append(String... content) {
        for (String s : content) {
            stringBuilder.append(s);
        }
        return this;
    }

    public LinedStringBuilder appendLine(String... content) {
        append(content).append("\n");
        return this;
    }

    public LinedStringBuilder append(Object... content) {
        for (Object o : content) {
            stringBuilder.append(o);
        }
        return this;
    }

    public LinedStringBuilder appendLine(Object... content) {
        append(content).append("\n");
        return this;
    }

    public LinedStringBuilder appendLine(String content) {
        stringBuilder.append(content).append("\n");
        return this;
    }

    public LinedStringBuilder appendLine(Object content) {
        return appendLine(String.valueOf(content));
    }

    public LinedStringBuilder appendLine() {
        stringBuilder.append("\n");
        return this;
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
