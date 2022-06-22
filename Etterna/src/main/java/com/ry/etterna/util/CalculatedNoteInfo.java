package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.MinaCalc;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.useful.StreamUtils;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java class created on 21/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@CommonsLog
public class CalculatedNoteInfo implements MinaCalculated {

    /**
     * The note info that this calculates the MSD for.
     */
    private final EtternaNoteInfo info;

    /**
     * The score goal to use when calculating the MSD of a chart.
     */
    private float scoreGoal = MinaCalc.DEFAULT_SCORE_GOAL;

    /**
     * The raw notes of this chart.
     */
    private final MinaCalc.RawNotes notes;

    /**
     * @param info The note info to calculate the MSD for.
     */
    public CalculatedNoteInfo(final EtternaNoteInfo info) {
        this.info = info;
        if (info.getCurTimingInfo() == null || !info.getParent().getTimingInfo().equals(info.getCurTimingInfo())) {
            info.timeNotesWith(info.getParent().getTimingInfo());
        }
        this.notes = new MinaCalc.RawNotes(info);
    }

    /**
     * Creates a list of Calculated Note infos from the base etterna charts.
     *
     * @param file The etterna file to process to calculated note info.
     * @return List of all difficulties wrapped in an ArrayList.
     */
    public static List<CalculatedNoteInfo> from(final EtternaFile file) {
        return file.getNoteInfo()
                .stream()
                .map(CalculatedNoteInfo::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MSD> getMSDForRate(final String rate) {

        try {
            return Optional.of(MSD.initFromFloats(MinaCalc.getMSDForRateAndGoal(
                    notes.getNotes(), notes.getTimes(),
                    getScoreGoal(), Float.parseFloat(rate)
            )));

        } catch (final Exception ex) {
            log.fatal("Mina Calc encountered a fatal exception", ex);
            System.exit(-1);
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<MSDChart> streamRateInRange(final BigDecimal min, final BigDecimal max) {

        if (min.compareTo(new BigDecimal("0.7")) < 0) {
            throw new IllegalStateException("Rate " + min + " unsupported.");
        }

        if (max.compareTo(new BigDecimal("2.0")) > 0) {
            throw new IllegalStateException("Rate " + max + " unsupported.");
        }

        return StreamUtils.createStreamLazily(supplyInfoInRange(min, max));
    }

    /**
     * Creates a MSDChart Supplier that supplies rates in a range.
     *
     * @param min Min value.
     * @param max Max value.
     * @return Lazy Supplier.
     */
    private Supplier<MSDChart> supplyInfoInRange(final BigDecimal min,
                                                 final BigDecimal max) {
        final AtomicReference<BigDecimal> i = new AtomicReference<>(min);
        final BigDecimal increment = new BigDecimal("0.05");

        return () -> {

            if (i.get().compareTo(max) <= 0) {
                final String rate = i.get().toPlainString();
                final Optional<MSD> msd = getMSDForRate(rate);
                i.set(i.get().add(increment, MathContext.DECIMAL64));

                return MSDChart.of(rate, msd.orElse(null), getInfo());
            } else {
                return null;
            }
        };
    }
}
