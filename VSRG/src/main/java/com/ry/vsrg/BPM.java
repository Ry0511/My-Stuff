package com.ry.vsrg;

import com.ry.vsrg.sequence.TimedElement;
import lombok.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class BPM extends TimedElement<BigDecimal> {

    /**
     * Constructor for a Timed element that starts, and finishes at the same
     * time.
     *
     * @param time The start and end time of this element.
     * @param value The value of this element.
     */
    public BPM(final @NonNull BigDecimal time,
               final @NonNull BigDecimal value) {
        super(time, value);
    }

    /**
     * Increases the rate of this BPM by the provided Rate.
     *
     * @param rate The rate of increase, or decrease by such as 1.1 or 0.7 of
     * which would make it faster, or slower.
     * @return New BPM with the value of the rated result.
     */
    public BPM rated(final BigDecimal rate) {
        return new BPM(
                getStartTime(),
                getValue().multiply(rate, MathContext.DECIMAL64)
        );
    }

    /**
     * Does this BPM occur before the provided Other BPM, that is it starts
     * earlier.
     *
     * @param other The other BPM.
     * @return {@code true} if the BPM starts before the provided other.
     */
    public boolean startsBefore(final BPM other) {
        // this start time is less than other
        return getStartTime().compareTo(other.getStartTime()) < 0;
    }

    /**
     * @return The start time.
     */
    public BigDecimal getStartTime() {
        return super.getStartTime();
    }

    /**
     * @return The BPM value.
     */
    public BigDecimal getValue() {
        return super.getValue();
    }
}
