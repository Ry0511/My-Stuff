package com.ry.vsrg.sequence.struct;

import com.ry.vsrg.sequence.Timed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@Builder
@AllArgsConstructor
public class Measure<N extends Timed> {

    //
    // I don't really like the idea of having the structure be modified, that
    // is, remove notes, or add them, or add measures, etc so a Builder is
    // used to create a Mostly Immutable Measure and if modifications are
    // needed then a completely new measure should be created.
    //

    /**
     * All the note rows in this measure.
     */
    private final Row<N>[] rows;

    /**
     * @return The size of this Measure.
     */
    public int size() {
        return rows.length;
    }

    /**
     * @return Stream of all Note Rows.
     */
    public Stream<Row<N>> stream() {
        return Stream.of(rows);
    }

    /**
     * For all Rows in this Measure apply the given action.
     *
     * @param action The action.
     */
    public void forAllRows(final Consumer<Row<N>> action) {
        stream().forEach(action);
    }
}
