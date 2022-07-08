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
public enum HitType {
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
