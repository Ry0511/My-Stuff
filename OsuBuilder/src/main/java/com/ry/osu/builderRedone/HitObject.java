package com.ry.osu.builderRedone;

import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.osu.builderRedone.sound.SampleSet;
import com.ry.osu.builderRedone.sound.Volume;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Value
@Builder(setterPrefix = "set", toBuilder = true)
public class HitObject {

    /**
     * The x position of this note.
     */
    int x;

    /**
     * The y position of this note.
     */
    int y;

    /**
     * The start time of this note.
     */
    @NonNull
    BigDecimal startTime;

    /**
     * The hit type of this note.
     */
    @NonNull
    HitType type;

    /**
     * The hit-sound for this note.
     */
    int hitSound;

    /**
     * The end time of this note; can be null.
     */
    BigDecimal endTime;

    /**
     * Object args.
     */
    @NonNull
    ObjectArgs args;

    public String compile() {
        return String.format(
                "%s,%s,%s,%s,%s,%s%s",
                getX(),
                getY(),
                getStartTime().toBigInteger().toString(),
                getType().getId(),
                getHitSound(),
                getEndTime() == null ? "" : getEndTime().toBigInteger().toString() + ":",
                args.compile()
        );
    }

    public static class HitObjectBuilder {

        public static final MathContext C = MathContext.DECIMAL64;
        public static final BigDecimal MS_TIME_SCALE = new BigDecimal("1000");
        private static final short MANIA_HOLD_Y = 192;

        /**
         * Sets the X position of this hit object as if it were a Mania note.
         *
         * @param col Column (0 -> k).
         * @param columns The number of columns available.
         * @return The builder.
         */
        public HitObjectBuilder setX(final int col, final int columns) {
            this.x = (int) ((512f / columns) * (col + 1) - (256f / columns));
            return this;
        }

        public HitObjectBuilder setX(final int x) {
            this.x = x;
            return this;
        }

        public HitObjectBuilder setType(final HitType type) {
            this.type = type;

            if (type == HitType.MANIA_HOLD) {
                setY(MANIA_HOLD_Y);
            }

            if (type == HitType.HIT) {
                setY(0);
            }

            return this;
        }

        public HitObjectBuilder setHitSound(final int s) {
            this.hitSound = s;
            return this;
        }

        public HitObjectBuilder setHitSound(final HitSound s) {
            this.hitSound = s.getId();
            return this;
        }

        public HitObjectBuilder setStartTime(final BigDecimal time) {
            this.startTime = time;
            return this;
        }

        public HitObjectBuilder setStartTime(final BigDecimal time, final boolean isInSeconds) {

            if (isInSeconds) {
                this.startTime = time.multiply(MS_TIME_SCALE, C);
            } else {
                this.startTime = time;
            }

            return this;
        }

        public HitObjectBuilder setEndTime(final BigDecimal time) {
            this.endTime = time;
            return this;
        }

        public HitObjectBuilder setEndTime(final BigDecimal time, final boolean isInSeconds) {

            if (time == null) {
                this.endTime = null;
            } else if (isInSeconds) {
                this.endTime = time.multiply(MS_TIME_SCALE, C);
            } else {
                this.endTime = time;
            }

            return this;
        }

        public HitObjectBuilder runIf(Predicate<HitObjectBuilder> predicate,
                                      Runnable action) {
            if (predicate.test(this)) {
                action.run();
            }
            return this;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public BigDecimal getStartTime() {
            return this.startTime;
        }

        public BigDecimal getEndTime() {
            return this.endTime;
        }

        public HitType getType() {
            return this.type;
        }

        public ObjectArgs getArgs() {
            return this.args;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Hit object arguments. This is probably mania specific I honestly have no
    // idea what the other modes use/do.
    ///////////////////////////////////////////////////////////////////////////

    @Value
    @Builder(setterPrefix = "set")
    public static class ObjectArgs {
        @Builder.Default
        SampleSet sampleSet = SampleSet.AUTO;
        @Builder.Default
        SampleSet additionSet = SampleSet.AUTO;
        int index;
        int volume;
        String hitSampleFile;

        public String compile() {
            return String.join(":", new String[]{
                    sampleSet == null ? "" : "" + sampleSet.getId(),
                    additionSet == null ? "" : "" + additionSet.getId(),
                    "" + index,
                    "" + volume,
                    hitSampleFile == null ? "" : hitSampleFile
            });
        }

        public static class ObjectArgsBuilder {

            public ObjectArgsBuilder initAuto() {
                setIndex(HitSound.HIT);
                setVolume(Volume.FULL);
                return this;
            }

            public ObjectArgsBuilder setIndex(final HitSound... sounds) {
                index = Arrays.stream(sounds)
                        .distinct()
                        .mapToInt(HitSound::getId)
                        .sum();
                return this;
            }

            public ObjectArgsBuilder setVolume(final Volume volume) {
                this.volume = volume.getLevel();
                return this;
            }
        }
    }
}
