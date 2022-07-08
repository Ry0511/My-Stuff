package com.ry.etterna.note;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.MinaCalc;
import com.ry.etterna.reader.EtternaTiming;
import com.ry.useful.MessageBuilder;
import com.ry.useful.MutatingValue;
import com.ry.useful.StringUtils;
import com.ry.vsrg.BPM;
import com.ry.vsrg.sequence.TimingSequence;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java class created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class EtternaNoteInfo {

    /**
     * Magic. This will probably create rounding errors on some files, but ima
     * take a gamble nobody will have a bpm with a unit value of this (1.0 -
     * 0.374643)
     */
    private static final double NUMBER_PROBABLY_PULLED_OUT_OF_SOMEONES_ASS
            = 0.374643;

    /**
     * Regex that matches the header info of .SM files.
     */
    private static final String HEADER_REGEX = "(?is)#NOTES:(.*?:)+";

    /**
     * Matches any comment style bodies.
     */
    private static final String COMMENT_REGEX = "[^,]*//.*";

    /**
     * The maximum number of columns to allow, this is only used to map LN's in
     * order to retain column information in memory.
     */
    private static final int MAX_COLUMNS = 99;

    /**
     * Hacky solution to appending the steps type information to the file.
     */
    private static final String DANCE_SINGLE_REGEX = "(?i)dance-single";

    /**
     * The parent file for this etterna chart.
     */
    @Setter
    private EtternaFile parent;

    /**
     * The current etterna timing information mapped for this note info.
     */
    private EtternaTiming curTimingInfo;

    /**
     * The string used to construct this note info.
     */
    private final String rawInfo;

    /**
     * Array of all Note Measures mapped.
     */
    private final NoteMeasure[] measures;

    /**
     * True if the steps-type for this chart dance-single.
     */
    private final boolean isDanceSingle;

    /**
     * The difficulty number of this chart.
     */
    @Setter
    private int difficultyIndex;

    /**
     * Loads the Note Info from the RAW string.
     *
     * @param info The string to load from.
     * @return New instance of etterna note info.
     */
    public static EtternaNoteInfo loadFromStr(final String info) {
        // Info is a single instance of: #NOTES:.*?;|$
        // pretty sure i couldn't just split at ':' and it would've been fine...
        final String[] measures = MutatingValue.of(info)
                .mutateValue((s) -> s.replaceFirst(HEADER_REGEX, "")
                        .replaceAll(COMMENT_REGEX, "")
                        .replaceAll("[\t\f ]+", ""))

                .mutateValueIf((s) -> s.endsWith(";"),
                        (s) -> s.substring(0, s.length() - 1))

                .mutateValue(String::trim)
                .mutateType((s) -> s.split(","))
                .getValue();

        // Load measures, rows, and notes
        return new EtternaNoteInfo(
                info,
                loadNotesFromStr(measures),
                StringUtils.contains(info, DANCE_SINGLE_REGEX)
        );
    }

    /**
     * Loads the notes into a single string.
     *
     * @param measures The measures to load.
     * @return All measures loaded to a complex type.
     */
    private static NoteMeasure[] loadNotesFromStr(final String[] measures) {
        final NoteMeasure[] pMeasures = new NoteMeasure[measures.length];
        for (int i = 0; i < measures.length; ++i) {
            pMeasures[i] = NoteMeasure.initFromStr(measures[i].trim());
        }
        return pMeasures;
    }

    /**
     * @return Stream of all Measures.
     */
    public Stream<NoteMeasure> stream() {
        return Arrays.stream(measures);
    }

    /**
     * Convenience method for iterating through all measures and rows of the
     * Note Info.
     *
     * @param action The action to apply to every Note Row.
     */
    public void forEachNote(final BiConsumer<NoteMeasure, NoteRow> action) {
        for (final NoteMeasure m : getMeasures()) {
            for (final NoteRow row : m) {
                action.accept(m, row);
            }
        }
    }

    /**
     * Lazily calculates the ChartKey for this NoteInfo with its current timing
     * data information.
     *
     * @return Chart key of current state of the note info.
     * @throws IllegalStateException If this timing info has not yet been
     *                               timed.
     */
    public String getChartKey() {

        if (curTimingInfo == null) {
            throw new IllegalStateException("Chart not timed: " + this);
        }

        final StringBuilder blob = new StringBuilder();
        forEachNote((measure, row) -> {
            if (!row.isEmpty()) {

                // For all non-empty rows
                row.stream().forEach(note -> {
                    if (!note.getStartNote().isHoldTail()) {
                        blob.append(note.getStartNote().ordinal());
                    } else {
                        blob.append(0);
                    }
                });

                // Magic number
                blob.append(
                        (int) (row.getBpm().getValue().doubleValue()
                                + NUMBER_PROBABLY_PULLED_OUT_OF_SOMEONES_ASS)
                );
            }
        });

        return "X" + DigestUtils.sha1Hex(blob.toString());
    }

    /**
     * Lazily calculates the ChartKey for this NoteInfo with its current timing
     * data information with a Strictly 4K approach, that is, any columns after
     * the fourth are ignored.
     *
     * @return Chart key of current state of the note info.
     * @throws IllegalStateException If this timing info has not yet been
     *                               timed.
     */
    public String getChartKey4K() {

        if (curTimingInfo == null) {
            throw new IllegalStateException("Chart not timed: " + this);
        }

        //
        // This exists because Etterna treats Dance-single as 4k, even if the
        // number of columns mapped is greater.
        //

        final int keyCount = 4;
        final StringBuilder blob = new StringBuilder();
        forEachNote((measure, row) -> {

            if (!row.stream()
                    .limit(keyCount)
                    .map(Note::getStartNote)
                    .allMatch(x -> x.isEmpty() || x.isHoldTail())) {

                // For all non-empty rows
                row.stream().map(Note::getStartNote)
                        .limit(keyCount).forEach(note -> {
                            if (!note.isHoldTail()) {
                                blob.append(note.ordinal());
                            } else {
                                blob.append(0);
                            }
                        });

                // Magic number
                blob.append(
                        (int) (row.getBpm().getValue().doubleValue()
                                + NUMBER_PROBABLY_PULLED_OUT_OF_SOMEONES_ASS)
                );
            }
        });

        return "X" + DigestUtils.sha1Hex(blob.toString());
    }

    /**
     * Compiles all notes into a debug string.
     *
     * @return String containing the BPM, TIME, and NOTES for every Note Row.
     */
    public String debugStr() {
        final MessageBuilder builder = MessageBuilder.builder(16);

        builder.add("BPM", "TIME", "NOTES").addLineSeparator();

        this.forEachNote((measure, row) -> {

            // Load notes
            final StringBuilder notesStr = new StringBuilder();
            row.stream().forEach(i -> {
                notesStr.append(i.getStartNote().getIdentityChar());
            });

            // Can throw nullPtr if not timed
            builder.add(row.getBpm().getValue().toPlainString())
                    .add(row.getStartTime().toPlainString())
                    .add(notesStr.toString())
                    .addLineSeparator();
        });

        return builder.build();
    }

    /**
     * Times this note info with the provided timing data.
     *
     * @param timingInfo The info to time this note info with.
     * @return This same reference for clarity of an internal change.
     */
    public EtternaNoteInfo timeNotesWith(
            @NonNull final EtternaTiming timingInfo) {
        this.curTimingInfo = timingInfo;
        return timeNotes(timingInfo);
    }

    /**
     * Times the note data information for this chart with the provided timing
     * info.
     *
     * @param timingInfo The new timing info.
     * @return This note info mutated.
     */
    private EtternaNoteInfo timeNotes(@NonNull final EtternaTiming timingInfo) {
        final TimingSequence sequence = new TimingSequence();

        // Hold flags
        final Note[] flags = new Note[MAX_COLUMNS];

        // For all notes
        for (final NoteMeasure measure : this.getMeasures()) {
            for (final NoteRow row : measure.getRows()) {

                // Map notes
                final AtomicInteger index = new AtomicInteger(0);
                row.stream().forEach(note -> {
                    note.setStartTime(sequence.getCurTimeScaled());

                    // Can obtain Head/Tail for all Long notes
                    if (note.getStartNote().isHold()) {
                        final Note flagged = flags[index.get()];

                        // Open LN
                        if (flagged == null) {
                            flags[index.get()] = note;

                            // Close LN
                        } else {
                            flagged.setEndTime(sequence.getCurTimeScaled());
                            flagged.setEndNote(note.getStartNote());
                            note.setEndNote(flagged.getStartNote());
                            flags[index.get()] = null;
                        }
                    }

                    index.getAndIncrement();
                });

                // Update timing
                final BPM curBpm
                        = timingInfo.getLatest(sequence.getCurBeatScaled());
                row.setBpm(curBpm);

                sequence.advanceByNote(
                        measure.size(),
                        curBpm.getValue()
                );
            }
        }

        return this;
    }

    /**
     * Gets the Cached step information for this chart. For this to work the
     * Chart must be timed using the 1.0x rate timing information.
     *
     * @param db The open database to query
     * @return Empty if the key didn't exist, else optional of a single result.
     * @throws SQLException If any occur whilst making/attempting the query.
     * @implNote It is not guaranteed that the Cached steps result is for this
     * chart, however we can assert that all Note based data is the same for
     * all.
     */
    public Optional<CacheStepsResult> queryStepsCache(
            @NonNull final CacheDB db) throws SQLException {
        final String key = isDanceSingle ? getChartKey4K() : getChartKey();
        return db.getStepCacheFor(key);
    }

    /**
     * Lazily counts the number of Rows in this chart.
     *
     * @return The number of Rows in this chart.
     */
    public int getNumRows() {
        int count = 0;

        for (final var v : getMeasures()) {
            count += v.size();
        }

        return count;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Used to calculate MSD in place for the current timing data.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return MSD info for the default 1.0 Rate (1.0 is based on the current
     * timing info)
     * @throws IllegalStateException If the notes have not yet been timed with
     *                               any timing data.
     */
    public MSD calcMSD() {
        if (getCurTimingInfo() == null) throw new IllegalStateException();

        final MinaCalc.RawNotes x = new MinaCalc.RawNotes(this);
        return MSD.initFromFloats(MinaCalc.getDefaultMSDFor(
                x.getNotes(),
                x.getTimes()
        ));
    }

    /**
     * @param scoreGoal The score-goal to achieve.
     * @param rate The desired MSD Rate.
     * @return MSD info for the provided rate and score goal.
     * @throws IllegalStateException If the notes have not yet been timed with
     *                               any timing data.
     */
    public MSD calcMSD(final float scoreGoal, final float rate) {
        if (getCurTimingInfo() == null) throw new IllegalStateException();

        final MinaCalc.RawNotes x = new MinaCalc.RawNotes(this);
        return MSD.initFromFloats(MinaCalc.getMSDForRateAndGoal(
                x.getNotes(), x.getTimes(),
                scoreGoal, rate
        ));
    }

    /**
     * Calculates the MSD for all rates 0.7 to 2.0 in increments of 0.1 using
     * the default 0.93 score-goal.
     *
     * @return List of MSD Values.
     */
    public List<MSD> calcMSDForAllRates() {
        if (getCurTimingInfo() == null) throw new IllegalStateException();

        final MinaCalc.RawNotes x = new MinaCalc.RawNotes(this);
        return MinaCalc.getMSDForAllRates(
                        x.getNotes(), x.getTimes(), new ArrayList<>())
                .stream()
                .map(MSD::initFromFloats)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
