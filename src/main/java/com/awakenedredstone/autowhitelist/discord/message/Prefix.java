package com.awakenedredstone.autowhitelist.discord.message;

import com.awakenedredstone.prechecks.RequireFieldsFrom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Forces a prefix into every registered id. \
/// Useful when using a shared predefined id and {@link RequireFieldsFrom}
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Prefix {
    String value();
}
