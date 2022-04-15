package com.ry.etterna.note;

import com.ry.vsrg.sequence.Timed;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Java class created on 11/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class Note implements Timed {

    /**
     * The time that this note occurs at.
     */
    private BigDecimal startTime;

    /**
     * The time that this note ends at.
     */
    private BigDecimal endTime;

    /**
     * The note of which this starts with.
     */
    private NoteType startNote;

    /**
     * The note of which this ends at.
     */
    private NoteType endNote;

    /**
     * The column that this note resides in (0 based).
     */
    private int column;

    public Note(final NoteType type) {
        this.startNote = type;
        this.endNote = type;
    }

    /**
     * @return The start time of this timed element.
     */
    @Override
    public BigDecimal getStartTime() {
        return startTime;
    }

    /**
     * @return The end time of this timed element.
     */
    @Override
    public BigDecimal getEndTime() {
        return endTime;
    }
}
