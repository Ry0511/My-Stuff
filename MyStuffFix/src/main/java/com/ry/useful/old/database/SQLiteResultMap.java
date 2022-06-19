package com.ry.useful.old.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java annotation created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SQLiteResultMap {

    /**
     * @return {@code true} if the JVM language checks for anything in this
     * class are overridden, that is, constructs like final or Private are
     * ignored when evaluating.
     */
    boolean isOverrideJvm() default false;
}
