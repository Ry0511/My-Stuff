package com.ry.etterna.note;

import com.ry.vsrg.sequence.struct.Row;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Java class created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class NoteRow extends Row<Note> implements Iterable<Note> {

    /**
     * Takes a RAW String of an Etterna note row and processes into a Note Row
     * instance.
     *
     * @param rawRow The raw note row.
     * @return Newly instantiated note row.
     */
    public static NoteRow loadFromStr(final String rawRow) {
        // Input String: '0130' etc

        if (!NoteType.isOnlyReserved(rawRow)) {
            throw new IllegalStateException("Not entirely known char "
                    + "sequence: " + rawRow);
        }

        final Note[] notes = new Note[rawRow.length()];
        int index = 0;
        for (final char c : rawRow.toCharArray()) {
            notes[index] = new Note(NoteType.of(String.valueOf(c)));
            notes[index].setColumn(index);
            ++index;
        }

        return new NoteRow(notes);
    }

    /**
     * @param notes The notes of this row.
     */
    public NoteRow(final @NonNull Note... notes) {
        super(notes);
        if (notes.length == 0) throw new IllegalStateException(
                "Must have atleast a single note column..."
        );
    }

    /**
     * @return {@code true} iff all Notes for this Row are Hold Head, or Empty.
     */
    public boolean isEmpty() {
        for (final Note note : getNotes()) {
            switch (note.getStartNote()) {
                case TAP:
                case HOLD_HEAD:
                case FAKE:
                case MINE:
                case AUTO_KEY_SOUND:
                case LIFT:
                    return false;
            }
        }

        return true;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Note> iterator() {
        return Arrays.stream(getNotes()).iterator();
    }

    /**
     * @return The start time of the note in the first column.
     */
    public BigDecimal getStartTime() {
        return getNotes()[0].getStartTime();
    }
}
