package com.ry.etterna.msd;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Java enum created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@AllArgsConstructor
public enum SkillSet {
    OVERALL("MSD"),
    STREAM("STR"),
    JUMP_STREAM("JS"),
    HAND_STREAM("HS"),
    STAMINA("STAM"),
    JACKS("JA"),
    CHORDS("CH"),
    TECHNICAL("TECH");

    /**
     * The number of skillsets to consider.
     */
    public static final int NUM_SKILLSETS = values().length;

    /**
     * The acronym of this ordinals name.
     */
    private final String acronym;
}
