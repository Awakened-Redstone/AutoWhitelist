package com.awakenedredstone.prechecks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RequireFieldsFrom {
    Class<?> value();
    int modifiers() default Modifier.STATIC | Modifier.FINAL;
}
