package com.ry.osu.builderRedone.util;

import com.ry.osu.builderRedone.TimingPoint;
import com.ry.vsrg.BPM;
import com.ry.vsrg.sequence.Timed;
import com.ry.vsrg.sequence.struct.Row;

/**
 * Java interface created on 24/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface TimingPointMapper<T extends Timed> {

    /**
     * Maps a new BPM value to a timing point.
     *
     * @param row The parent row of the BPM.
     * @param bpm The BPM value of the row.
     * @return null if this BPM should be skipped, else a new TimingPoint.
     */
    TimingPoint mapToTimingPoint(final Row<T> row, final BPM bpm);
}
