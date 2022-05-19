package com.ry.vsrg.sequence.struct;

import com.ry.vsrg.BPM;
import com.ry.vsrg.sequence.Timed;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.stream.Stream;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@ToString
@EqualsAndHashCode
public class Row<T extends Timed> {

    /**
     * The BPM encapsulating this Note Row and all of its Notes.
     */
    @Setter
    private BPM bpm;

    /**
     * All notes of this Row.
     */
    private final T[] notes;

    /**
     * @param notes The notes of this row.
     */
    @SafeVarargs
    public Row(@NonNull final T... notes) {

        // Technically should allow this however I will assume that there
        // should be an "Empty" variant of type T
        if (notes.length == 0) {
            throw new IllegalStateException(
                    "Empty Note Row: " + toString()
            );
        }
        this.notes = notes;
    }

    /**
     * @return Stream of all Notes in this row of notes.
     */
    public Stream<T> stream() {
        return Stream.of(getNotes());
    }

    /**
     * @return The number of notes in this row.
     */
    public int size() {
        return notes.length;
    }
}
