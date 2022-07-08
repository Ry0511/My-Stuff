package com.ry.osu.builderRedone.sound;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
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
