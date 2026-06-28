package com.awakenedredstone.autowhitelist.util.object;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;
import java.util.function.Function;

@NullMarked
public class DataFlow {
    public static <T, R> @Nullable R nullableF(@Nullable T value, Function<T, @Nullable R> ifNotNull) {
        if (value == null) return null;
        return ifNotNull.apply(value);
    }

    public static <T> void nullableC(@Nullable T value, Consumer<T> ifNotNull) {
        if (value == null) return;
        ifNotNull.accept(value);
    }
}
