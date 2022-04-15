package com.ry.useful.functional;

/**
 * Java interface created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public interface EXFunction<T, R> {
    R apply(T t) throws Exception;
}
