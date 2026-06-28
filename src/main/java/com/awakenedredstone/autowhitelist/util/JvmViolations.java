package com.awakenedredstone.autowhitelist.util;

import org.jetbrains.annotations.ApiStatus;

/**
 * A set of methods that should never exist and bypass the compiler,
 * leaving problems for the JVM to deal with, and probably crash.
 */
@ApiStatus.Internal
public class JvmViolations {
    /// Bypass the compiler type checks and cast a class to any other class. \
    /// This may crash at runtime if used with incompatible types.
    /// @param o The class to be cast
    /// @return the class cast as T
    /// @param <T> the type to be cast to
    @SuppressWarnings("unchecked")
    public static <T> T unsafeCast(Object o) {
        return (T) o;
    }

    /// Bypass the compiler type checks and cast a class to any subclass. \
    /// This may crash at runtime if used with incompatible types.
    /// @param o The class to be cast
    /// @return the class cast as T
    /// @param <T> the type to be cast to
    @SuppressWarnings("unchecked")
    public static <O, T extends O> T upperCast(O o) {
        return (T) o;
    }
}
