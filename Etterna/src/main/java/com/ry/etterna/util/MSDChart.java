package com.ry.etterna.util;

import com.ry.etterna.msd.MSD;
import com.ry.etterna.note.EtternaNoteInfo;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Java class created on 20/06/2022 for usage in project My-Stuff. Let's pollute
 * the heap with small objects.
 *
 * @author -Ry
 */
@Value(staticConstructor = "of")
public class MSDChart {

    /**
     * The rate of this chart. This is not nullable.
     */
    @NonNull
    String rate;

    /**
     * The msd value for this rate. This is nullable.
     */
    MSD msd;

    /**
     * The note info for this chart. This is not nullable.
     */
    @NonNull
    EtternaNoteInfo info;

    /**
     * @return {@code true} if the MSD is present, that is, the MSD of this
     * chart is not null.
     */
    public boolean isMSDPresent() {
        return msd != null;
    }

    /**
     * Since timing the etterna info can be costly its omitted until absolutely
     * needed. This method will time the etterna note info with its default
     * timing data however to current rate.
     */
    public void accept() {
        info.timeNotesWith(info.getParent().getTimingInfo().rated(new BigDecimal(rate)));
    }
}
