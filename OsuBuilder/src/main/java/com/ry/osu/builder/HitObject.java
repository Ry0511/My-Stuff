package com.ry.osu.builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * Java class created on 24/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@NoArgsConstructor
public class HitObject {

    // Probably should've designed this to be immutable, however, in all
    // likelihood I will create one using the no args constructor and then
    // update each item from there conditionally.

    /**
     * The X position of this Hit object.
     */
    private int x;

    /**
     * The y position of this Hit object.
     */
    private int y;

    /**
     * The time that this Hit Object should be hit.
     */
    private String time;

    /**
     * The type of Hit object that this is.
     */
    private int type;

    /**
     * The hit-sound for this Hit Object.
     */
    private int hitSound;

    /**
     * Optional end time for an Osu!Mania hold.
     */
    private String endTime;

    /**
     * Colon delimited args for the hit sample. The ordering is:
     * normalSet:additionSet:index:volume:filename.
     */
    private final String[] hitSample = {
            "0", "0", "0", "0", ""
    };

    /**
     * Sets X position to the correct pixel value based on the provided
     * mania note column.
     *
     * @param col The desired note column (Zero based).
     * @param columns The total number of columns.
     */
    public void setManiaColumn(final int col, final int columns) {
        this.x = (int) ((512f / columns) * (col + 1) - (256f / columns));
    }

    /**
     * Sets the type of the note to the provided type.
     *
     * @param type The note type.
     */
    public void setType(final Type type) {
        this.type = type.getId();
    }

    /**
     * Sets the hit sound to the provided sound.
     *
     * @param s The sound.
     */
    public void setHitSound(final Sound s) {
        this.hitSound = s.getId();
    }

    /**
     * Sets the hit sample set.
     *
     * @param set The sample set to use.
     */
    public void setHitSample(final SampleSet set) {
        hitSample[0] = "" + set.getId();
    }

    /**
     * Sets the additional sample set  to the provided set.
     *
     * @param set The sample set to use.
     */
    public void setAdditionalSample(final SampleSet set) {
        hitSample[1] = "" + set.getId();
    }

    /**
     * Sets the index sound to the provided sound.
     *
     * @param sound The sound index.
     */
    public void setIndex(final Sound sound) {
        hitSample[2] = "" + sound.getId();
    }

    /**
     * Sets the hit-sound volume to the provided volume level (0 to 100).
     *
     * @param volume The volume level.
     */
    public void setVolume(final int volume) {
        this.hitSample[3] = "" + volume;
    }

    /**
     * Sets the hit-sound volume to the provided volume.
     *
     * @param volume The volume level.
     */
    public void setVolume(final Volume volume) {
        setVolume(volume.getLevel());
    }

    /**
     * Poor mans builder.
     *
     * @param actions The actions to apply to this.
     */
    @SafeVarargs
    public final void set(final Consumer<HitObject>... actions) {
        for (final Consumer<HitObject> act : actions) {
            act.accept(this);
        }
    }

    /**
     * @return This hit object as an osu string.
     */
    public String asOsuStr() {
        return String.format(
                "%s,%s,%s,%s,%s,%s%s",
                getX(),
                getY(),
                getTime(),
                getType(),
                getHitSound(),
                getEndTime() == null ? "" : getEndTime() + ":",
                String.join(":", getHitSample())
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Hit object inner data mappings
    ///////////////////////////////////////////////////////////////////////////

    @Getter
    @AllArgsConstructor
    public enum Type {
        // Probably right lol
        HIT(Integer.parseInt("1", 2)),
        SLIDER(Integer.parseInt("00000010", 2)),
        SPINNER(Integer.parseInt("00001000", 2)),
        MANIA_HOLD(Integer.parseInt("10000000", 2));

        /**
         * The numerical id for this Hit object type.
         */
        private final int id;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Sound {
        NORMAL(0),
        WHISTLE(1),
        FINISH(2),
        CLAP(3);

        /**
         * The numerical id of this type.
         */
        private final int id;
    }

    @Getter
    @AllArgsConstructor
    public enum SampleSet {
        NONE(0),
        NORMAL(1),
        SOFT(2),
        DRUM(3);

        /**
         * The numerical id of this sample set.
         */
        private final int id;
    }

    @Getter
    @AllArgsConstructor
    public enum Volume {
        FULL(100),
        HALF(50),
        NONE(0);

        /**
         * The volume level.
         */
        private final int level;
    }
}
