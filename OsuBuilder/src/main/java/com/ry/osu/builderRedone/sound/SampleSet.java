package com.ry.osu.builderRedone.sound;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

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

    public static SampleSet from(final int ss) {
        return Arrays.stream(values())
                .filter(x -> x.getId() == ss)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sample set '" + ss + "' doesn't exist."));
    }
}
