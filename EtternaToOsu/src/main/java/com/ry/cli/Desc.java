package com.ry.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java annotation created on 30/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Desc {
    String value();
    boolean insertNewLine() default false;
}
