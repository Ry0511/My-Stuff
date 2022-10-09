package com.ry.useful.property;

/**
 * Java interface created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public interface Mapper<V> {

    /**
     * Maps the provided property value to a Type.
     *
     * @param value The raw value to map.
     * @return The mapped value type.
     */
    V map(String value);
}
