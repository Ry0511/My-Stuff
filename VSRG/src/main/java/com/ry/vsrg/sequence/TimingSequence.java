package com.ry.vsrg.sequence;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Java class created on 16/03/2022 for usage in project VSRGObjects.
 *
 * @author -Ry
 */
@Getter
public class TimingSequence {

    // todo Scaling the output values to 3DP can result in error especially
    //  if the chart has a very large BPM at some points.

    /**
     * Time signatures used by ArrowVortex and Etterna, this is primarily used
     * to debug the file reader since I know if a sporadic beat length occurs
     * that I probably should manually assess that file.
     */
    private static final int[] KNOW_SIGNATURES = {
            4, 8, 12, 16, 20, 24, 32, 48, 64, 96, 192
    };

    /**
     * The base time signature that all calculations are normalised against.
     */
    private static final double BASE_SIGNATURE = 4;

    /**
     * The base timescale for the sequence by default this the normal timescale
     * of 60s -> minute though other options such as 60000ms -> minute though it
     * is just scaling up by v * 1000.
     */
    private static final double TIME_SCALE = 60.0;

    /**
     * Milliseconds scale value for converting the base values into
     * Milliseconds.
     */
    private static final int MS_TIME_SCALE = 1000;

    /**
     * Flag used to indicate that the sequence is in debug mode, that is, iff
     * true then exceptions are propagated in awkward situations.
     */
    private static final boolean IS_DEBUG_MODE = false;

    /**
     * The current time of this sequence normalised against 60s.
     */
    private BigDecimal curTime = new BigDecimal("0.0", MathContext.DECIMAL64);

    /**
     * The current beat of this timing sequence normalised against 4/4 measure.
     */
    private BigDecimal curBeat = new BigDecimal("0.0", MathContext.DECIMAL64);

    /**
     * Throws an exception iff the provided signature is unknown.
     *
     * @param signature The signature to check.
     */
    private static void check(final int signature) {
        if (!IS_DEBUG_MODE) return;

        for (final int v : KNOW_SIGNATURES) {
            if (signature == v) return;
        }

        throw new Error("Potentially unsafe Signature: " + signature);
    }

    /**
     * Advances this timing sequence by a single note in the provided measure at
     * the provided BPM.
     *
     * @param timeSignature The measure/signature for the note such as 1/4 or
     * 1/k where this is 'k'
     * @param bpm The Beats Per Minute to use for calculations.
     */
    public void advanceByNote(final int timeSignature,
                              final double bpm) {
        check(timeSignature);

        if ((timeSignature <= 0) || (bpm <= 0)) {
            throw new Error(String.format(
                    "Time Signature and BPM must be positive. Signature: %s; "
                            + "BPM: %s...",
                    timeSignature,
                    bpm
            ));

        } else {
            // Not sure if this is required or if I can just use D64 however
            // we clamp at the 3rd radix, so it doesn't matter that much.
            final MathContext c = new MathContext(12, RoundingMode.HALF_EVEN);

            curTime = curTime.add(
                    calcTimePerNote(timeSignature, bpm),
                    c
            );
            curBeat = curBeat.add(
                    calcBeatPerNote(timeSignature),
                    MathContext.DECIMAL64
            );
        }
    }

    /**
     * @see #advanceByNote(int, double)
     */
    public void advanceByNote(final int timeSignature,
                              final BigDecimal bpm) {
        advanceByNote(timeSignature, bpm.doubleValue());
    }

    /**
     * Takes the target value and adds the provided value using {@link
     * MathContext#DECIMAL64} as the math context.
     *
     * @param target The target to add to.
     * @param value The value to add.
     * @return target + value using the DECIMAL64 variant.
     */
    private BigDecimal bigDecimalAdd(final BigDecimal target,
                                     final double value) {
        final BigDecimal v = new BigDecimal(
                String.valueOf(value),
                MathContext.DECIMAL64
        );

        return target.add(v, MathContext.DECIMAL64);
    }

    /**
     * @return The current Beat value scaled to 2 decimals using a Half even
     * approach.
     */
    public BigDecimal getCurBeatScaled() {
        final int defaultScale = 2;

        return curBeat.setScale(defaultScale, RoundingMode.UP);
    }

    /**
     * @return The current time scaled to 3 decimal places using a Floor
     * approach.
     */
    public BigDecimal getCurTimeScaled() {
        final int defaultScale = 3;

        return curTime.setScale(defaultScale, RoundingMode.FLOOR);
    }

    public BigDecimal getCurTimeScaledFloat() {
        return curTime.setScale(7, RoundingMode.FLOOR);
    }

    /**
     * @return The current time elapsed as Milliseconds.
     */
    public BigDecimal getCurTimeScaledMS() {
        // Get, scale, clamp
        return new BigDecimal(getCurTimeScaled()
                .multiply(BigDecimal.valueOf(MS_TIME_SCALE))
                .toBigInteger()
                .toString()
        );
    }

    /**
     * Calculates the time for a single note at the provided measure and bpm.
     *
     * @param measure The current measure of the notes such as 1/4 or 1/8
     * @param bpm The current number of beats per minute.s
     * @return The time per note, such that, v * bpm == Scale, where v is the
     * time per note.
     */
    public static BigDecimal calcTimePerNote(final int measure,
                                             final double bpm) {
//        final MathContext c = new MathContext(12, RoundingMode.HALF_EVEN);
        final MathContext c = MathContext.DECIMAL64;

        final BigDecimal a
                = BigDecimal.valueOf(TIME_SCALE)
                .divide(BigDecimal.valueOf(bpm), c);

        final BigDecimal b
                = BigDecimal.valueOf(measure)
                .divide(BigDecimal.valueOf(BASE_SIGNATURE), c);


        // (60 / 196) / (32 / 4) =
        // (TIME_SCALE / BPM) / (SIGNATURE / BASE_SIGNATURE)
        return a.divide(b, c);
    }

    /**
     * Gets the beat per note for the provided measure normalised against v/4.
     *
     * @param measure The current measure to get the beat for.
     * @return The beat per note, such that, beat * measure == 1.0
     */
    private static BigDecimal calcBeatPerNote(final int measure) {
//        final MathContext c = new MathContext(12, RoundingMode.HALF_EVEN);
        final MathContext c = MathContext.DECIMAL64;

        return BigDecimal.valueOf(BASE_SIGNATURE)
                .divide(BigDecimal.valueOf(measure), c);
    }
}
