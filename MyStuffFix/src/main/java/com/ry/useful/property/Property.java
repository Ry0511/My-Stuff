package com.ry.useful.property;

/**
 * Java interface created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public interface Property {

    /**
     * Extracts this property from the String content.
     *
     * @param content The content to extract this property from.
     * @return The extracted property.
     */
    ExtractedProperty extract(String content);
}
