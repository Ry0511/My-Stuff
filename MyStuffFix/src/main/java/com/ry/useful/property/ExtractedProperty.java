package com.ry.useful.property;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class ExtractedProperty {

    /**
     * The property that has been extracted.
     */
    private final Property extractedProperty;

    /**
     * The raw extracted values for this property.
     */
    @Getter(AccessLevel.PRIVATE)
    private final List<String> rawValues;

    /**
     * Constructs an empty property.
     *
     * @param p The property.
     */
    public static ExtractedProperty empty(final Property p) {
        return new ExtractedProperty(p);
    }

    /**
     * Constructs the extracted property from the base property and the
     * extracted values.
     *
     * @param p The property that was extracted.
     * @param values The extracted values.
     * @return New instance.
     */
    public static ExtractedProperty of(final Property p,
                                       final String... values) {
        return new ExtractedProperty(p, values);
    }

    /**
     * Empty constructor.
     *
     * @param property The property.
     */
    private ExtractedProperty(final Property property) {
        this.extractedProperty = property;
        this.rawValues = new ArrayList<>();
    }

    /**
     * Constructs the extracted property from the base property and the
     * extracted values.
     *
     * @param property The property that was extracted.
     * @param values The extracted values.
     */
    private ExtractedProperty(final Property property,
                              final String... values) {
        this.extractedProperty = property;
        this.rawValues = Arrays.asList(values);
    }

    /**
     * @return Iff isSingleton then the only value is returned.
     * @throws Error Iff not Singleton.
     */
    public String getSingleton() {
        if (!isSingleton())
            throw new Error("Property not Singleton: " + toString());
        return rawValues.get(0);
    }

    /**
     * Processes a singleton value to a mapped value type.
     *
     * @param mapper The mapper to aggregate the singleton value.
     * @param <V> The type of the mapped value.
     * @return The mapped value.
     */
    public <V> V processSingleton(final Mapper<V> mapper) {
        return mapper.map(getSingleton());
    }

    /**
     * @return All the values for this property.
     */
    public String[] getMany() {
        if (!hasMany()) throw new Error("Property Not Many: " + toString());
        return this.rawValues.toArray(new String[0]);
    }

    /**
     * Processes many values to a mapped type.
     *
     * @param mapper The mapper which maps the values.
     * @param <V> The type of the mapped value.
     * @return List of mapped values.
     */
    public <V> List<V> processMany(final Mapper<V> mapper) {
        final List<V> vs = new ArrayList<>();
        this.rawValues.forEach(i -> vs.add(mapper.map(i)));
        return vs;
    }

    /**
     * @return {@code true} if the number of values held for this property is
     * exactly one.
     */
    public boolean isSingleton() {
        return rawValues.size() == 1;
    }

    /**
     * @return {@code true} if the number of values held for this property is
     * exactly zero.
     */
    public boolean isEmpty() {
        return rawValues.size() == 0;
    }

    /**
     * @return {@code true} if the number of values held for this property is
     * more than 1.
     */
    public boolean hasMany() {
        return rawValues.size() > 1;
    }
}
