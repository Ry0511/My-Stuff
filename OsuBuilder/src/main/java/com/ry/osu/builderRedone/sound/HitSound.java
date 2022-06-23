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
public enum HitSound {
    HIT(0),
    WHISTLE(2),
    FINISH(4),
    CLAP(8);

    /**
     * The numerical id of this type.
     */
    private final int id;
}
