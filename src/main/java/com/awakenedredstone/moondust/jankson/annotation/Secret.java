package com.awakenedredstone.moondust.jankson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the field as secret. This will make so it is replaced with the annotation value on serialization.<br/>
 * This is only meant to prevent leaking sensitive values on logging or debug commands. <br/>
 * This will not override all serialization, you can set the JSON grammar to show the secrets. <br/>
 * <br/>
 * <b>Please note that values serialized with secrets hidden may not be deserializable.</b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Secret {
    String value() default "[secret]";
}
