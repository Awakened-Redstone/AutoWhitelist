package com.awakenedredstone.autowhitelist.util;

import org.jetbrains.annotations.ApiStatus;

/// A set of methods to deal with the compiler.
@ApiStatus.Internal
public class CompilerWorkarounds {
    /// Bypass the compiler type checks and cast a class to any other class. \
    /// This will crash at runtime if used with incompatible types.
    /// @param o The class to be cast
    /// @return the class cast as T
    /// @param <T> the type to be cast to
    @SuppressWarnings("unchecked")
    public static <T> T unsafeCast(Object o) {
        return (T) o;
    }

    /// Bypass the compiler type checks and cast a class to any subclass. \
    /// This will crash at runtime if used with incompatible types.
    /// @param o The class to be cast
    /// @return the class cast as T
    /// @param <T> the type to be cast to
    @SuppressWarnings("unchecked")
    public static <O, T extends O> T upperCast(O o) {
        return (T) o;
    }
}
