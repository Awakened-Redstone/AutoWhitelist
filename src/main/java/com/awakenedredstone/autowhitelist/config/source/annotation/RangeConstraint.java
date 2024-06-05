package com.awakenedredstone.autowhitelist.config.source.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeConstraint {
    double min();
    double max();
    boolean minInclusive() default true;
    boolean maxInclusive() default true;
    boolean minusOne() default false;
}
