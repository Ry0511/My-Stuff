package com.ry.vsrg.sequence;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter(AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TimedElement<T> {

    /**
     * The time that this timed element begins at.
     */
    private final BigDecimal startTime;

    /**
     * The time that this timed element ends at.
     */
    private final BigDecimal endTime;

    /**
     * The timed element.
     */
    private T value;

    /**
     * Constructor for a Timed element that starts, and finishes at the same
     * time.
     *
     * @param time The start and end time of this element.
     * @param value The value of this element.
     */
    public TimedElement(@NonNull final BigDecimal time,
                        @NonNull final T value) {
        this.startTime = time;
        this.endTime = time;
        this.value = value;
    }
}
