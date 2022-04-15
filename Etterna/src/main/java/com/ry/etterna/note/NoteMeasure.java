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
            if (numCols == -1) {
                numCols = noteRows[index].getNotes().length;
            }

            // Number of columns should stay consistent
            if (numCols != noteRows[index].getNotes().length) {
                throw new IllegalStateException(String.format(
                        "Note Row Malformed expected size '%s' but got '%s'%n",
                        numCols,
                        noteRows[index].toString()
                ));
            }

            ++index;
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
