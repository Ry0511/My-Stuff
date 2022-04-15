package com.ry.vsrg.sequence;

import java.math.BigDecimal;

/**
 * Java interface created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public interface Timed {

    /**
     * @return The start time of this timed element.
     */
    BigDecimal getStartTime();

    /**
     * @return The end time of this timed element.
     */
    BigDecimal getEndTime();
}
