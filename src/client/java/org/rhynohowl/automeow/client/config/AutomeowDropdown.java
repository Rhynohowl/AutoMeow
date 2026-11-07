package org.rhynohowl.automeow.client.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutomeowDropdown {
    Source value();

    enum Source {
        BADGE_STYLE,
        GRADIENT_PRESET,
        GRADIENT_DIRECTION,
        REPLY_MODE,
        REPLY_TEXT
    }
}