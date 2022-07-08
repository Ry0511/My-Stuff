package com.ry.etterna.util;

import com.ry.etterna.msd.MSD;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Java interface created on 20/06/2022 for usage in project My-Stuff. Interface
 * which wraps the basic MSD obtain methods that were introduced in the
 * CachedNoteInfo however due to not wanting to strictly use Cached MSD values
 * interfacing the class for backwards compatability is done so that versions
 * can be swapped with ease.
 *
 * @author -Ry
 */
public interface MinaCalculated {

    public static BigDecimal MIN_RATE = new BigDecimal("0.5");
    public static BigDecimal HALF_TIME_RATE = new BigDecimal("0.7");
    public static BigDecimal MAX_RATE = new BigDecimal("2.0");

    /**
     * Gets the MSD value for the provided rate.
     *
     * @param rate The rate of the chart.
     * @return MSD value if present, else empty.
     */
    Optional<MSD> getMSDForRate(String rate);

    /**
     * Creates a stream populated with this chart and its MSD values for all
     * rates in the provided range.
     *
     * @param min The min rate.
     * @param max The max rate.
     */
    Stream<MSDChart> streamRateInRange(BigDecimal min, BigDecimal max);

    /**
     * Creates a stream which will be populated with all rates for this chart
     * and their associative MSD value.
     */
    default Stream<MSDChart> streamRateInRange() {
        return streamRateInRange(HALF_TIME_RATE, MAX_RATE);
    }
}
