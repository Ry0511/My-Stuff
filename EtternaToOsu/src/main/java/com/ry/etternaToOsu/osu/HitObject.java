package com.ry.etternaToOsu.osu;

import com.ry.etterna.note.Note;
import com.ry.useful.Constants;
import lombok.Data;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class HitObject {

    ///////////////////////////////////////////////////////////////////////////
    // Slapped this together in 10 minutes so hush.
    ///////////////////////////////////////////////////////////////////////////

    private static final String BASE_TAP_NOTE_STR
            = "%s,%s,%s,%s,0,%s:0:0:100:";

    private static final String BASE_HOLD_NOTE_STR
            = "%s,%s,%s,%s,0,%s:%s:%s:0:0:";

    private final String hitObj;

    public HitObject(final Note note) {
        final int factor = 1000;
        final int column = pixelPosCol(note.getColumn());

        final String start = Constants.factorDecimal(
                note.getStartTime(),
                factor
        ).toBigInteger().toString();

        // Hold note
        if (note.getStartNote().isHoldHead()) {
            final String end = Constants.factorDecimal(
                    note.getEndTime(),
                    factor
            ).toBigInteger().toString();
            this.hitObj = compileLN(column, start, end);
            return;
        }

        // Tap note
        if (note.getStartNote().isTap()) {
            this.hitObj = compileHit(column, start);
            return;
        }

        throw new Error("Hit Object Setup Failed: " + note);
    }

    private static int pixelPosCol(final int col) {
        return switch (col) {
            case 0 -> 64;
            case 1 -> 192;
            case 2 -> 320;
            case 3 -> 448;
            default -> -1;
        };
    }

    private String compileHit(final int column,
                              final String startTime) {
        return String.format(
                BASE_TAP_NOTE_STR,
                column,
                0,
                startTime,
                1,
                "3"
        );
    }

    private String compileLN(final int column,
                             final String startTime,
                             final String endTime) {
        return String.format(
                BASE_HOLD_NOTE_STR,
                column,
                192,
                startTime,
                128,
                endTime,
                "3",
                "3"
        );
    }
}
