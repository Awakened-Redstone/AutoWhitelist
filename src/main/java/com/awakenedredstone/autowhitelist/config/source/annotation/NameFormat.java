package com.awakenedredstone.autowhitelist.config.source.annotation;

import com.google.common.base.CaseFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NameFormat {
    Case value();

    enum Case {
        CAMEL_CASE(CaseFormat.LOWER_CAMEL),
        PASCAL_CASE(CaseFormat.UPPER_CAMEL),
        SNAKE_CASE(CaseFormat.LOWER_UNDERSCORE),
        SCREAMING_SNAKE_CASE(CaseFormat.UPPER_UNDERSCORE);

        private final CaseFormat caseFormat;

        Case(CaseFormat caseFormat) {
            this.caseFormat = caseFormat;
        }

        public CaseFormat getCaseFormat() {
            return caseFormat;
        }
    }
}
