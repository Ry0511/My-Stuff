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
public enum HitSound {
    HIT(0),
    WHISTLE(2),
    FINISH(4),
    CLAP(8);

    /**
     * The numerical id of this type.
     */
    private final int id;

    public static HitSound from(final int si) {
        return Arrays.stream(values())
                .filter(x -> x.getId() == si)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Hitsound '" + si + "' doesn't exist."));
    }
}
