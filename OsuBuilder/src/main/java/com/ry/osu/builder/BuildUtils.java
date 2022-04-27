package com.ry.osu.builder;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.note.Note;
import com.ry.etterna.note.NoteMeasure;
import com.ry.etterna.note.NoteRow;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.useful.Entity;
import com.ry.vsrg.BPM;
import lombok.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class created on 24/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public final class BuildUtils {

    /**
     * Milliseconds scale factor.
     */
    private static final BigDecimal MILLIS_FACTOR
            = new BigDecimal("1000.0", MathContext.DECIMAL64);

    /**
     * Cached etterna files will use the default etterna naming pack structure
     * and MSD filters.
     *
     * @param cache The cache file to create from.
     * @return Pre-built-Builder with the following data set: Hit Objects,
     * Timing Points, Title, Tags, and Source.
     */
    public static BuildableOsuFile.BuildableOsuFileBuilder fromEtternaCache(
            @NonNull final CachedNoteInfo cache,
            @NonNull final MSD msd) {
        final TimingContainer container = getHitInfo(cache.getInfo());

        final EtternaFile f = cache.getEtternaFile();

        return BuildableOsuFile.builder()
                .setHitObjects(container.hitObjects())
                .setTimingPoints(container.timingPoints())
                .setTitle(f.getPackFolder().getName())
                .setTags(msd.getMsdFilterTag("18", "38", "1"))
                .setSource(msd.debugStr());
    }

    /**
     * Loads a mania timing container from the provided note info.
     *
     * @param info The info to load.
     * @return All hit objects, and relevant timing points.
     */
    public static TimingContainer getHitInfo(final EtternaNoteInfo info) {
        final List<TimingPoint> timing = new ArrayList<>();
        final List<HitObject> hit = new ArrayList<>();

        final Entity<BigDecimal> bpm = Entity.empty();
        info.forEachNote((measure, row) -> {

            // Load timing data
            final BigDecimal v = row.getBpm().getValue();
            if (!bpm.isPresent() || (bpm.getValue().compareTo(v) != 0)) {
                bpm.setValue(row.getBpm().getValue());
                timing.add(bpmToTimingPoint(measure, row));
            }

            // Load Note Row
            for (final Note note : row) {
                if (note.getStartNote().isTap()
                        || note.getStartNote().isHoldHead()) {
                    hit.add(noteToHitObject(note, row.size()));
                }
            }
        });

        return new TimingContainer(hit, timing);
    }

    /**
     * Converts a mania row & measure into an osu! timing point.
     *
     * @param m The measure.
     * @param r The row.
     * @return New timing point.
     */
    private static TimingPoint bpmToTimingPoint(final NoteMeasure m,
                                                final NoteRow r) {
        final BPM bpm = r.getBpm();
        final TimingPoint tp = new TimingPoint();

        tp.setBeatLength(bpm.getValue());
        tp.setMeter(m.size());
        tp.setUnInherited(true);
        tp.setTime(r.getStartTime()
                .multiply(MILLIS_FACTOR, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toString());
        tp.setVolume(HitObject.Volume.HALF.getLevel());

        return tp;
    }

    /**
     * Loads the provided note as a osu!mania hit object.
     *
     * @param note The note to load.
     * @param numColumns The number of columns in a row.
     * @return New hit object instance.
     */
    private static HitObject noteToHitObject(final Note note,
                                             final int numColumns) {
        final HitObject ho = new HitObject();
        final int holdYPos = 192;
        final BigDecimal start = note.getStartTime()
                .multiply(MILLIS_FACTOR, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.UNNECESSARY);

        // Clamp end time
        final HitObject.Type type;
        BigDecimal end = note.getEndTime();
        if (note.getEndNote() != null) {
            ho.setEndTime(note.getEndTime()
                    .multiply(MILLIS_FACTOR, MathContext.DECIMAL64)
                    .toBigInteger()
                    .toString()
            );
            type = HitObject.Type.MANIA_HOLD;
        } else {
            type = HitObject.Type.HIT;
        }

        // Set internals
        ho.setTime(start.toString());
        ho.setManiaColumn(note.getColumn(), numColumns);
        ho.setY(end == null ? 0 : holdYPos);
        ho.setType(type);

        // Set base timing sounds
        ho.setHitSample(HitObject.SampleSet.NORMAL);
        ho.setHitSound(HitObject.Sound.FINISH);
        ho.setVolume(HitObject.Volume.FULL);

        return ho;
    }

    /**
     * Lazy container for data.
     */
    private record TimingContainer(List<HitObject> hitObjects,
                                   List<TimingPoint> timingPoints) {
    }
}
