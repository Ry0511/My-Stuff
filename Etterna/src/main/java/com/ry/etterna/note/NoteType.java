package com.ry.etterna.note;

import lombok.Getter;

/**
 * Java enum created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
public enum NoteType {
    EMPTY("[^1-4MLKF]", '0'),
    TAP("[1]", '1'),
    HOLD_HEAD("[24]", '2'),
    HOLD_TAIL("[3]", '3'),
    MINE("M", 'M'),
    LIFT("[L]", 'L'),
    AUTO_KEY_SOUND("[K]", 'K'),
    FAKE("[F]", 'F');

    /**
     * Regex of all the known reserved Etterna Note characters.
     */
    public static final String RESERVED_REGEX = "[0-4MLKF]";

    /**
     * The regex that matches this note type.
     */
    private final String identity;

    /**
     * The character to identify this Note Type.
     */
    private final char identityChar;

    /**
     * @param s The string to test for reserved characters.
     * @return {@code true} iff the provided string contains only reserved
     * characters.
     */
    public static boolean isOnlyReserved(final String s) {
        return s.replaceAll(RESERVED_REGEX, "").length() == 0;
    }

    /**
     * Finds an appropriate note type that matches the specified character.
     *
     * @param ch The aforementioned character.
     * @return First appropriate type that matched, in full.
     */
    public static NoteType of(final String ch) {
        for (final NoteType type : NoteType.values()) {
            if (type.isThis(ch)) {
                return type;
            }
        }

        throw new IllegalStateException("Unknown Note Type: " + ch);
    }

    /**
     * @param identity The identity regex.
     * @param identityChar The character that represents this note type.
     */
    NoteType(final String identity,
             final char identityChar) {
        this.identity = identity;
        this.identityChar = identityChar;
    }

    /**
     * @param s The string to test.
     * @return {@code true} is 's' matches this.
     */
    public boolean isThis(final String s) {
        return s.matches(this.identity);
    }

    /**
     * @return {@code true} iff this is {@link #EMPTY}.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * @return {@code true} iff this is TAP, AUTO_KEY_SOUND, or FAKE.
     */
    public boolean isTap() {
        return this == TAP || this == AUTO_KEY_SOUND || this == FAKE;
    }

    /**
     * @return {@code true} iff this is HOLD_HEAD, HOLD_TAIL.
     */
    public boolean isHold() {
        return this == HOLD_HEAD || this == HOLD_TAIL;
    }

    /**
     * @return {@code true} iff this is HOLD_HEAD.
     */
    public boolean isHoldHead() {
        return this == HOLD_HEAD;
    }

    /**
     * @return {@code true} iff this is HOLD_TAIL.
     */
    public boolean isHoldTail() {
        return this == HOLD_TAIL;
    }

    /**
     * @return {@code true} iff this is a Mine.
     */
    public boolean isMine() {
        return this == MINE;
    }
}
