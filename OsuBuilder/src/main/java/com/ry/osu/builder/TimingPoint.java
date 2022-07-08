package com.ry.osu.builder;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.Consumer;

/**
 * Java class created on 24/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@NoArgsConstructor
public class TimingPoint {

    /**
     * Start time of this timing point in MS.
     */
    private String time;

    /**
     * The beat length of each note.
     */
    private String beatLength;

    /**
     * The current measure/meter.
     */
    private int meter;

    /**
     * The sample set.
     */
    private int sampleSet;

    /**
     * The sample index.
     */
    private int sampleIndex;

    /**
     * The volume of the timing point.
     */
    private int volume;

    /**
     * 0 or 1; If 1 then it will be used in timing sequence calculations. If 0
     * it will only be used as a modifier (defaults to 1.0x).
     */
    private int unInherited;

    /**
     * Sets the beat length for this timing point as its BPM.
     *
     * @param bpm The bpm of this timing point.
     */
    public void setBeatLength(final BigDecimal bpm) {
        final MathContext c = MathContext.DECIMAL64;
        this.beatLength = BigDecimal.ONE
                .divide(bpm, c)
                .multiply(new BigDecimal("1000.0"), c)
                .multiply(new BigDecimal("60.0", c))
                .setScale(7, RoundingMode.UP)
                .toString();
    }

    /**
     * Sets the hit sample set.
     *
     * @param set The sample set to use.
     */
    public void setHitSample(final HitObject.SampleSet set) {
        sampleSet = set.getId();
    }

    /**
     * Sets the index sound to the provided sound.
     *
     * @param sound The sound index.
     */
    public void setIndex(final HitObject.Sound sound) {
        sampleIndex = sound.getId();
    }

    /**
     * Sets the is un-inherited flag.
     *
     * @param isUninherited True if not inherited.
     */
    public void setUnInherited(final boolean isUninherited) {
        unInherited = isUninherited ? 1 : 0;
    }

    /**
     * Poor mans builder.
     *
     * @param actions The actions to apply.
     */
    @SafeVarargs
    public final void set(final Consumer<TimingPoint>... actions) {
        for (final Consumer<TimingPoint> action : actions) {
            action.accept(this);
        }
    }

    /**
     * @return This timing point as an osu string.
     */
    public String asOsuStr() {
        return String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s",
                getTime(),
                getBeatLength(),
                getMeter(),
                getSampleSet(),
                getSampleIndex(),
                getVolume(),
                getUnInherited(),
                "0" // Effects are unused
        );
    }
}
