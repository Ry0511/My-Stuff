package com.ry.osu.builderRedone.util;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.osu.builderRedone.HitObject;
import com.ry.osu.builderRedone.TimingPoint;
import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.vsrg.BPM;
import com.ry.vsrg.sequence.Timed;
import com.ry.vsrg.sequence.struct.Measure;
import com.ry.vsrg.sequence.struct.Row;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@AllArgsConstructor
public class TimingInfo {

    private final List<TimingPoint> timingPoints;
    private final List<HitObject> hitObjects;

    // TODO Factory methods should provide a filter mechanism, as not all notes need to be mapped some should be skipped.

    public static <T extends Timed> TimingInfo loadFromNotes(final List<Measure<T>> measures,
                                                             final TimingPointMapper<T> timingPointMapper,
                                                             final HitObjectMapper<T> hitObjectMapper) {

        final List<TimingPoint> timingPoints = new ArrayList<>();
        final List<HitObject> hitObjects = new ArrayList<>();
        final AtomicReference<BPM> latestBpm = new AtomicReference<>();
        latestBpm.getAndSet(measures.get(0).getRows()[0].getBpm());

        // Add first timing point
        final TimingPoint tp0 = timingPointMapper.mapToTimingPoint(measures.get(0).getRows()[0], latestBpm.get());
        if (tp0 != null) {
            timingPoints.add(tp0);
        }

        measures.forEach(measure -> {
            for (final Row<T> row : measure.getRows()) {

                // BPM not the same
                if (!(latestBpm.get().getValue().compareTo(row.getBpm().getValue()) == 0)) {
                    latestBpm.getAndSet(row.getBpm());

                    // Map Timing Point
                    final TimingPoint tp = timingPointMapper.mapToTimingPoint(row, latestBpm.get());
                    if (tp != null) {
                        timingPoints.add(tp);
                    }
                }

                // Map Notes
                for (final T note : row.getNotes()) {
                    final HitObject hitObj = hitObjectMapper.mapToHitObj(measure, row, note);
                    if (hitObj != null) {
                        hitObjects.add(hitObj);
                    }
                }
            }
        });

        return new TimingInfo(timingPoints, hitObjects);
    }

    public static TimingInfo loadFromEtternaInfo(final EtternaNoteInfo info,
                                                 final UnaryOperator<TimingPoint.TimingPointBuilder> timingPointFinaliser,
                                                 final UnaryOperator<HitObject.HitObjectBuilder> hitObjectFinaliser) {
        return loadFromNotes(
                Arrays.asList(info.getMeasures()),
                // Map BPM
                (row, bpm) -> timingPointFinaliser.apply(TimingPoint
                        .builder()
                        .initDefaults()
                        .setTime(row.getNotes()[0].getStartTime(), true)
                        .setBeatLength(bpm.getValue())
                ).build(),
                // Map Notes
                (measure, row, note) -> {
                    if (note.getStartNote().isHoldHead() || note.getStartNote().isTap()) {
                        return hitObjectFinaliser.apply(HitObject
                                .builder()
                                .setStartTime(note.getStartTime(), true)
                                .setX(note.getColumn(), row.size())
                                .setY(0)
                                .setType(note.getStartNote().isHoldHead() ? HitType.MANIA_HOLD : HitType.HIT)
                                .setEndTime(note.getEndTime(), true)
                                .setHitSound(HitSound.HIT)
                                .setArgs(HitObject.ObjectArgs.builder().initAuto().build())
                        ).build();
                    } else {
                        return null;
                    }
                }
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Updating the data after creation.
    ///////////////////////////////////////////////////////////////////////////

    public void replaceTimingPoints(final Predicate<TimingPoint> filter,
                                    final Function<TimingPoint, TimingPoint> action) {
        for (int i = 0; i < getTimingPoints().size(); ++i) {
            final TimingPoint tp = getTimingPoints().get(i);
            if (filter.test(tp)) {
                timingPoints.set(i, action.apply(tp));
            }
        }
    }

    public void replaceHitObjects(final Predicate<HitObject> filter,
                                  final Function<HitObject, HitObject> action) {
        for (int i = 0; i < getHitObjects().size(); ++i) {
            final HitObject hitObj = getHitObjects().get(i);
            if (filter.test(hitObj)) {
                getHitObjects().set(i, action.apply(hitObj));
            }
        }
    }
}
