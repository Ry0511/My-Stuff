package com.ry.etterna.note;

import com.ry.vsrg.sequence.struct.Measure;
import com.ry.vsrg.sequence.struct.Row;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Java class created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class NoteMeasure extends Measure<Note> implements Iterable<NoteRow> {

    /**
     * Takes a RAW Etterna Note Measure and loads the underlying data.
     *
     * @param measure The RAW Note measure to load.
     * @return Loaded note measure that consists of none of the timing data only
     * the Note Types.
     */
    public static NoteMeasure initFromStr(final String measure) {
        final String[] rows = measure.split("\\s+");
        final NoteRow[] noteRows = new NoteRow[rows.length];

        // Load rows
        int index = 0;
        int numCols = -1;
        for (final String row : rows) {
            noteRows[index] = NoteRow.loadFromStr(row.trim());

            // Set size
            numCols = Math.max(numCols, noteRows[index].getNotes().length);

            ++index;
        }

        // Re-fill the array using the fixed size column data
        for (int i = 0; i < noteRows.length; ++i) {
            final Note[] notes = new Note[numCols];
            Arrays.fill(notes, new Note(NoteType.EMPTY));
            System.arraycopy(
                    noteRows[i].getNotes(),
                    0,
                    notes,
                    0,
                    noteRows[i].size()
            );
            noteRows[i] = new NoteRow(notes);
        }

        return new NoteMeasure(noteRows);
    }

    /**
     * Constructs the note measure from the Note Rows.
     *
     * @param rows The aforementioned note rows.
     */
    public NoteMeasure(@NonNull final Row<Note>[] rows) {
        super(rows);
    }

    @Override
    public NoteRow[] getRows() {
        return (NoteRow[]) super.getRows();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<NoteRow> iterator() {
        return Arrays.stream(getRows()).iterator();
    }
}
