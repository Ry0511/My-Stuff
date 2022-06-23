package com.ry.osu.builderRedone.sound;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Java enum created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Getter
@AllArgsConstructor
public enum SampleSet {
    AUTO(0),
    NORMAL(1),
    SOFT(2),
    DRUM(3);

    /**
     * The numerical id of this sample set.
     */
    private final int id;
}
