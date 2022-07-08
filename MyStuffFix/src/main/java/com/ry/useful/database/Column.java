package com.ry.useful.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java interface created on 06/04/2022 for usage in project FunctionalUtils.
 * Methods annotated by this are assumed to consume a single argument that
 * argument can be of Type Object, or an explicit type, when the class is
 * being loaded all methods annotated with Column are called to extract data
 * from a ResultSet.
 *
 * @author -Ry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Column {

    /**
     * @return The name of the column for this value.
     */
    String value();
}
