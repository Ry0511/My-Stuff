package com.ry.osu.builderRedone;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.note.Note;
import com.ry.etterna.note.NoteRow;
import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.vsrg.BPM;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class TimingInfo {

    private final List<TimingPoint> timingPoints;
    private final List<HitObject> hitObjects;

    public static TimingInfo loadFromEtternaInfo(final EtternaNoteInfo info,
                                                 final TimingPointMapper timingPointMapper,
                                                 final HitObjectMapper hitObjectMapper) {
        return new TimingInfo(info, (bpm) -> timingPointMapper.map(
                TimingPoint.builder()
                        .initDefaults()
                        .setTime(bpm.getStartTime(), true)
                        .setBeatLength(bpm.getValue()),
                bpm

        ), (note, row) -> hitObjectMapper.map(
                HitObject.builder()
                        .setStartTime(note.getStartTime(), true)
                        .setX(note.getColumn(), row.size())
                        .setY(0)
                        .setType(note.getEndTime() != null ? HitType.MANIA_HOLD : HitType.HIT)
                        .setEndTime(note.getEndTime(), true)
                        .setHitSound(HitSound.HIT)
                        .setArgs(HitObject.ObjectArgs.builder().initAuto().build()),
                note,
                row
        ));
    }

    /**
     * Constructs the timing info from the base info, and a set of mapping
     * functions which will map Etterna Objects to their Osu! counterpart.
     *
     * @param info The note info to map. This has to be timed.
     * @param timingPointMapper Maps a new BPM value, that is, one that has not
     * been encountered to an Osu! Timing point.
     * @param hitObjectMapper Maps an Etterna note, and the measure size to an
     * Osu! hit object.
     */
    public TimingInfo(final EtternaNoteInfo info,
                      final Function<BPM, TimingPoint> timingPointMapper,
                      final BiFunction<Note, NoteRow, HitObject> hitObjectMapper) {
        if (info.getCurTimingInfo() == null) {
            throw new IllegalStateException("Provided Note info has no timing information...");
        }
        timingPoints = new ArrayList<>();
        hitObjects = new ArrayList<>();

        final AtomicReference<BPM> prevBpm = new AtomicReference<>();
        info.forEachNote((measure, row) -> {

            if (prevBpm.get() == null || !prevBpm.get().getValue().equals(row.getBpm().getValue())) {
                prevBpm.getAndSet(row.getBpm());
                timingPoints.add(timingPointMapper.apply(prevBpm.get()));
            }

            for (final Note note : row) {
                if (note.getStartNote().isTap() || note.getStartNote().isHoldHead()) {
                    hitObjects.add(hitObjectMapper.apply(note, row));
                }
            }
        });
    }

    public static interface TimingPointMapper {
        TimingPoint map(TimingPoint.TimingPointBuilder builder, BPM bpm);
    }

    public static interface HitObjectMapper {
        HitObject map(HitObject.HitObjectBuilder builder, Note note, NoteRow row);
    }
}
