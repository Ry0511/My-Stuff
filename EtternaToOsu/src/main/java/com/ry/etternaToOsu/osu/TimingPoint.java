package com.ry.etternaToOsu.osu;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class TimingPoint {

    ///////////////////////////////////////////////////////////////////////////
    // This class looks cryptic to all hell I just got lazy and slapped it
    // together in 10 mins ok...
    ///////////////////////////////////////////////////////////////////////////

    private StringJoiner args = new StringJoiner(",");

    public TimingPoint(@NonNull final BigDecimal startTime,
                       @NonNull final BigDecimal bpm,
                       @NonNull final int meter,
                       @NonNull final boolean isInherit) {
        final Object[] args = {
          startTime, bpm, meter, null, null, null, isInherit ? 0 : 1, null
        };
        final Elem[] xs = Elem.values();
        for (int i = 0; i < args.length; ++i) {
            setElem(xs[i], args[i]);
        }
    }

    private void setElem(final Elem elem, final Object value) {
        args.add(elem.handle.apply(value));
    }

    @RequiredArgsConstructor
    public enum Elem {
        START_TIME(x -> startTime((BigDecimal) x)),
        BEAT_LENGTH(x -> beatLength((BigDecimal) x)),
        METER(x -> String.valueOf(x)),
        SAMPLE_SET(x -> "1"),
        SAMPLE_INDEX(x -> "0"),
        VOLUME(x -> "100"),
        UN_INHERITED(x -> String.valueOf(x)),
        EFFECTS(x -> "0");

        private static String beatLength(BigDecimal bpm) {
            final MathContext c = MathContext.DECIMAL64;
            return new BigDecimal("1.0")
                    .divide(bpm, c)
                    .multiply(new BigDecimal("1000.0"), c)
                    .multiply(new BigDecimal("60.0", c))
                    .setScale(7, RoundingMode.UP)
                    .toString();
        }

        private static String startTime(final BigDecimal time) {
            final MathContext c = MathContext.DECIMAL64;
            return time.multiply(new BigDecimal("1000.0"), c)
                    .toBigInteger()
                    .toString();
        }

        private final Function<Object, String> handle;
    }
}
