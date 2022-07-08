package com.ry.osu.builderRedone.util;

import com.ry.osu.builderRedone.HitObject;
import com.ry.vsrg.sequence.Timed;
import com.ry.vsrg.sequence.struct.Measure;
import com.ry.vsrg.sequence.struct.Row;

/**
 * Java interface created on 24/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface HitObjectMapper<T extends Timed> {

    /**
     * Maps a note, in a row, in a measure to an Osu! Hit Object.
     *
     * @param measure The measure.
     * @param row The row.
     * @param note The note.
     * @return Hit Object, or null, if null it is assumed to skip this note.
     */
    HitObject mapToHitObj(final Measure<T> measure, final Row<T> row, final T note);
}
