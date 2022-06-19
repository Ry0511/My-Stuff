package com.ry.useful.old.property;

import com.ry.useful.MutatingValue;
import com.ry.useful.StringUtils;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class SimpleStringProperty {

    /**
     * The original string property loaded, that is, unmodified.
     */
    private final String raw;

    /**
     * Empty state for this property. Defaults to false, unless creation is done
     * via the static empty() method.
     */
    private final boolean isEmpty;

    /**
     * @return Debug value for missing properties.
     */
    public static SimpleStringProperty empty() {
        return new SimpleStringProperty("__MISSING__", true);
    }

    private SimpleStringProperty(final String raw,
                                 final boolean isEmpty) {
        this.raw = raw;
        this.isEmpty = isEmpty;
    }

    public SimpleStringProperty(final String raw) {
        this.raw = raw;
        this.isEmpty = raw == null || raw.isEmpty();
    }

    /**
     * @return The RAW String minus any new lines, or excess spaces.
     */
    public String getProcessed() {
        // Cleanse newlines & excess spaces
        return raw.replaceAll("[\n\r]+", "").trim();
    }

    /**
     * Maps the raw value.
     *
     * @param mapper The mapper to use.
     * @param <R> The type of the mapped value.
     * @return Mapped value.
     */
    public <R> R mapRaw(@NonNull final Mapper<R> mapper) {
        return mapper.map(raw);
    }

    /**
     * Maps the processed value.
     *
     * @param mapper The mapper to use.
     * @param <R> The type of the mapped result.
     * @return The result from the mapping.
     */
    public <R> R mapProcessed(@NonNull final Mapper<R> mapper) {
        return mapper.map(getProcessed());
    }

    /**
     * Processes the RAW string using the provided mapper, and then maps it.
     *
     * @param process The string processor.
     * @param mapper The mapper.
     * @param <R> The type of the final mapped value.
     * @return The mapped result.
     */
    public <R> R processAndMap(@NonNull final Mapper<String> process,
                               @NonNull final Mapper<R> mapper) {
        return mapper.map(process.map(raw));
    }

    /**
     * @return A potential decimal value if a mapping could be made.
     * @throws NumberFormatException Cuz i'm lazy.
     */
    public Optional<BigDecimal> asDecimal() {
        var result =
                asMutatingValue(SimpleStringProperty::getProcessed)
                        .mutateValue(x -> x.replaceAll("[^0-9.-]+", ""))
                        .mutateTypeIf(StringUtils::isFloat, BigDecimal::new)
                        .getMutated();

        if (result == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(result.getValue());
        }
    }

    /**
     * @param action The string to use when creating a mutating value.
     * @return Mutating value of the result from the provided action.
     */
    public MutatingValue<String> asMutatingValue(
            @NonNull final Function<SimpleStringProperty, String> action) {
        return MutatingValue.of(action.apply(this));
    }

    /**
     * @return This as a mutating value.
     */
    public MutatingValue<SimpleStringProperty> asMutatingValue() {
        return MutatingValue.of(this);
    }
}
