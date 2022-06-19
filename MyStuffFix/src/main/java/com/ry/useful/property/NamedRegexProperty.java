package com.ry.useful.property;

import com.ry.useful.StringUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class NamedRegexProperty implements Property {

    /**
     * The regular expression for this regex property.
     */
    @EqualsAndHashCode.Exclude
    private final Pattern regex;

    /**
     * The capture group used when extracting this property.
     */
    @EqualsAndHashCode.Exclude
    private final int group;

    /**
     * The name of this property.
     */
    private final String name;

    /**
     * The mode to use when extracting this property.
     */
    @EqualsAndHashCode.Exclude
    private final ExtractionMode mode;

    /**
     * Extracts this property from the String content.
     *
     * @param content The content to extract this property from.
     * @return The extracted property.
     */
    @Override
    public ExtractedProperty extract(final String content) {
        switch (mode) {
            // Only one should be found
            case SINGLETON -> {
                return getSingleton(content);
            }

            // Many can be found
            case MANY -> {
                return getMany(content);
            }

            // null or smth
            default -> throw new Error("Unknown Mode: " + mode);
        }
    }

    /**
     * Loads a single property from the content string.
     *
     * @param content The string to extract from.
     * @return Extracted property which is either Empty, or contains at MOST one
     * entry, that is, {@link ExtractedProperty#isSingleton()} is true or {@link
     * ExtractedProperty#isEmpty()} is true.
     */
    private ExtractedProperty getSingleton(final String content) {
        final String extracted = StringUtils.get(
                regex.matcher(content),
                Matcher::find,
                (m) -> m.group(group)
        ).orElse(null);

        // Extract property
        if (extracted == null || extracted.isEmpty()) {
            return ExtractedProperty.empty(this);

        } else {
            return ExtractedProperty.of(this, extracted);
        }
    }

    /**
     * Extracts many properties, that is 0, 1, 2, all the way to infinity.
     *
     * @param content The string to extract from.
     * @return Extracted property that is either Empty, Singleton, or Many.
     */
    private ExtractedProperty getMany(final String content) {
        final List<String> extracted = StringUtils.getAll(
                regex.matcher(content),
                Matcher::find,
                (m) -> m.group(group)
        );

        // Extract property
        if (extracted.isEmpty()) {
            return ExtractedProperty.empty(this);

        } else {
            return ExtractedProperty.of(this, extracted.toArray(new String[0]));
        }
    }

    /**
     * @return {@code true} if extraction mode is Singleton.
     */
    public boolean isSingleton() {
        return mode == ExtractionMode.SINGLETON;
    }

    /**
     * @return {@code true} if extraction mode is Many.
     */
    public boolean isMany() {
        return mode == ExtractionMode.MANY;
    }

    /**
     * Enumeration of the extraction modes possible for Regex properties, that
     * is, the regex can extract "Many" or "Just one" from a content string.
     *
     * @author -Ry
     * @version 0.1 Copyright: N/A
     */
    public enum ExtractionMode {
        SINGLETON,
        MANY;
    }
}
