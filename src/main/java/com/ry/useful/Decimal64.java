package com.ry.useful;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.Function;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class Decimal64 {

    /**
     * The actual value.
     */
    @Getter(AccessLevel.PRIVATE)
    private BigDecimal value;

    /**
     * Constructs the decimal from Zero.
     */
    public Decimal64() {
        value = new BigDecimal("0.0", MathContext.DECIMAL64);
    }

    /**
     * Constructs the decimal from the provided initial value.
     *
     * @param initValue Initial value.
     */
    public Decimal64(final double initValue) {
        value = new BigDecimal(initValue, MathContext.DECIMAL64);
    }

    /**
     * @return The current value.
     */
    public BigDecimal get() {
        return this.value;
    }

    /**
     * @param radix The new scale.
     * @return Gets the value with the provided radix using the default rounding
     * mode.
     */
    public BigDecimal get(final int radix) {
        return this.value.setScale(
                radix,
                MathContext.DECIMAL64.getRoundingMode()
        );
    }

    /**
     * @param radix The new scale.
     * @param mode The rounding mode.
     * @return Gets the value with the provided radix using the default rounding
     * mode.
     */
    public BigDecimal get(final int radix, final RoundingMode mode) {
        return this.value.setScale(radix, mode);
    }

    /**
     * @param v The value to process into BigDecimal.
     * @return Bigdecimal value.
     */
    private BigDecimal of(final double v) {
        return new BigDecimal(v, MathContext.DECIMAL64);
    }

    /**
     * @param v The value to add.
     * @return The new value.
     */
    public Decimal64 add(final double v) {
        this.value = this.value.add(of(v), MathContext.DECIMAL64);
        return this;
    }

    /**
     * @param v The value to subtract.
     * @return The new value.
     */
    public Decimal64 subtract(final double v) {
        this.value = this.value.subtract(of(v), MathContext.DECIMAL64);
        return this;
    }

    /**
     * Applies the given function to the current value.
     *
     * @param action The action to apply.
     * @return The new value.
     * @implNote You're responsible for ensuring that the computation is of
     * {@link MathContext#DECIMAL64}.
     */
    public Decimal64 apply(final Function<BigDecimal, BigDecimal> action) {
        this.value = action.apply(this.value);
        return this;
    }
}
