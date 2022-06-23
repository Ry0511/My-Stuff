package com.ry.osu.builderRedone;

import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.SampleSet;
import com.ry.osu.builderRedone.sound.Volume;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Value
@Builder(setterPrefix = "set", toBuilder = true)
public class TimingPoint {

    /**
     * Start time of this timing point in MS.
     */
    @NonNull
    BigDecimal time;

    /**
     * The beat length of each note.
     */
    @NonNull
    BigDecimal beatLength;

    /**
     * The current measure/meter.
     */
    int meter;

    /**
     * The sample set.
     */
    @NonNull
    SampleSet sampleSet;

    /**
     * The sample index.
     */
    @NonNull
    HitSound sampleIndex;

    /**
     * The volume of the timing point.
     */
    int volume;

    /**
     * If true this timing point affects the timings of the mapped notes if
     * false it does not.
     */
    boolean unInherited;

    /**
     * Compiles this timing point to the osu! expected format.
     *
     * @return Osu!Timing point string format.
     */
    public String compile() {
        return String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s",
                getTime().toBigInteger().toString(),
                getBeatLength().toPlainString(),
                getMeter(),
                getSampleSet().getId(),
                getSampleIndex().getId(),
                getVolume(),
                isUnInherited() ? 1 : 0,
                "0" // Effects are unused
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Builder.
    ///////////////////////////////////////////////////////////////////////////

    public static class TimingPointBuilder {

        public static final MathContext C = MathContext.DECIMAL64;
        public static final BigDecimal MS_TIME_SCALE = new BigDecimal("1000");
        public static final BigDecimal MAX_SECONDS = new BigDecimal("60");

        public TimingPointBuilder setTime(final BigDecimal time) {
            this.time = time;
            return this;
        }

        public TimingPointBuilder setTime(final BigDecimal time, final boolean isInSeconds) {
            if (isInSeconds) {
                this.time = time.multiply(MS_TIME_SCALE, C);
            } else {
                this.time = time;
            }
            return this;
        }

        public TimingPointBuilder setBeatLength(final BigDecimal bpm) {
            this.beatLength = BigDecimal.ONE
                    .divide(bpm, C)
                    .multiply(MS_TIME_SCALE, C)
                    .multiply(MAX_SECONDS, C)
                    .setScale(7, RoundingMode.UP);

            return this;
        }

        public TimingPointBuilder setVolume(final Volume volume) {
            this.volume = volume.getLevel();
            return this;
        }

        public TimingPointBuilder setVolume(final int volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Initialises the default volume, sample set, hit sound, and marks this
         * as an un-inherited timing point.
         *
         * @return This builder.
         * @see Volume#FULL
         * @see SampleSet#DRUM
         * @see HitSound#HIT
         */
        public TimingPointBuilder initDefaults() {
            setVolume(Volume.FULL);
            setSampleSet(SampleSet.DRUM);
            setSampleIndex(HitSound.HIT);
            setUnInherited(true);

            return this;
        }
    }
}
