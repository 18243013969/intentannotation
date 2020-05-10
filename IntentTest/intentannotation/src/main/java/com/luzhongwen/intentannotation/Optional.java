package com.luzhongwen.intentannotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;


@Retention(RetentionPolicy.SOURCE) @Target(FIELD)
public @interface Optional {
}
