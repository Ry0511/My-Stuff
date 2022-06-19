package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaTiming;
import lombok.Data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;


/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class CachedNoteInfo {

    /**
     * Rounding mode for single radix mutations.
     */
    private static final MathContext C
            = new MathContext(1, RoundingMode.FLOOR);

    /**
     * Full rate increment.
     */
    private static final BigDecimal FULL_RATE = new BigDecimal("0.10", C);

    /**
     * Half rate increment.
     */
    private static final BigDecimal HALF_RATE = new BigDecimal("0.05", C);

    /**
     * Cache information for the Note Info.
     */
    private final CacheStepsResult cache;

    /**
     * The Note Info for the cached notes.
     */
    private final EtternaNoteInfo info;

    /**
     * Loads from the provided etterna chart and a cache database all proper
     * note info objects.
     *
     * @param x The file to load from.
     * @param db The database to read cache from.
     * @return List of all cached files.
     */
    public static List<CachedNoteInfo> from(final EtternaFile x,
                                            final CacheDB db) {
        final List<CachedNoteInfo> xs = new ArrayList<>();
        if (x.isStandard()) {
            for (final EtternaNoteInfo info : x.getNoteInfo()) {
                if (info.isDanceSingle()) {
                    info.timeNotesWith(x.getTimingInfo());
                    try {
                        info.queryStepsCache(db).ifPresent(cache -> xs.add(
                                new CachedNoteInfo(cache, info)
                        ));

                        // Skip on fail
                    } catch (final SQLException e) {
                        System.err.printf(
                                "[SQL ERROR] Skipping: '%s' reason: '%s'%n",
                                x.getSmFile(), e.getMessage()
                        );
                    }
                }
            }
        }

        // For all bad cases return null
        return xs;
    }

    /**
     * @param cache The 1.0 cache.
     * @param info The timed note info.
     */
    public CachedNoteInfo(final CacheStepsResult cache,
                          final EtternaNoteInfo info) {
        this.cache = cache;
        this.info = info;
    }

    /**
     * @return The etterna file of this cached notes.
     */
    public EtternaFile getEtternaFile() {
        return this.info.getParent();
    }

    /**
     * @return The name of the pack.
     */
    public String getPackTitle() {
        return getEtternaFile().getPackFolder().getName();
    }

    /**
     * Gets the MSD for the provided rate. Note that this only accepts rates
     * within the 0.1 and 0.05 range, so values such as 0.15 and 0.95 are
     * allowed however may not produce entirely accurate results since they're
     * interpolated from the nearest range.
     *
     * @param rate The rate to get.
     * @return Optional containing the potential rate if present.
     * @throws IllegalStateException If the provided rate is not a 0.1 or 0.05
     *                               increment.
     */
    public Optional<MSD> getMSDForRate(final String rate) {
        // 0.0567899... -> 0.05
        final BigDecimal r = new BigDecimal(rate, MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_EVEN);

        // Iff r % 0.1 == 0
        if (r.remainder(FULL_RATE).compareTo(BigDecimal.ZERO) == 0) {
            return cache.getMSDForRate(rate);

            // Iff r % 0.05 == 0
        } else if (r.remainder(HALF_RATE).compareTo(BigDecimal.ZERO) == 0) {
            final Optional<MSD> lower = cache.getMSDForRate(
                    r.subtract(HALF_RATE, MathContext.DECIMAL64)
                            .setScale(2, RoundingMode.HALF_EVEN).toString()
            );
            final Optional<MSD> upper = cache.getMSDForRate(
                    r.add(HALF_RATE, MathContext.DECIMAL64)
                            .setScale(2, RoundingMode.HALF_EVEN).toString()
            );

            if (lower.isPresent() && upper.isPresent()) {
                return Optional.of(lower.get().interpolateMSD(upper.get()));
            }

            // Rate not allowed
        } else {
            throw new IllegalStateException("Unsupported Rate: " + rate);
        }

        return Optional.empty();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Iterating rates
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param min Min (Start).
     * @param max Max (End).
     * @param inc Increment.
     * @param rateHandle Action.
     */
    private void forEachRateInRange(final String min,
                                    final String max,
                                    final String inc,
                                    final Consumer<BigDecimal> rateHandle) {
        BigDecimal i = new BigDecimal(min);
        final BigDecimal ma = new BigDecimal(max);
        final BigDecimal increment = new BigDecimal(inc);

        while (i.compareTo(ma) <= 0) {
            rateHandle.accept(i);
            i = i.add(increment, MathContext.DECIMAL64);
        }
    }

    /**
     * For each rate in range which adheres to the MSD Filter rule, apply the
     * given action.
     *
     * @param min The minimum rate.
     * @param max The maximum rate.
     * @param msdFilter The MSD Filter, first argument is the 1.0 MSD, and the
     * second one is the k-rate MSD.
     * @param action The action to apply if the filter is true.
     */
    public void forEachRate(final String min,
                            final String max,
                            final BiPredicate<MSD, MSD> msdFilter,
                            final RatedChartHandler action) {
        final MSD normal
                = getMSDForRate("1.0").orElseThrow(RuntimeException::new);
        final EtternaTiming baseTiming = getEtternaFile().getTimingInfo();

        forEachRateInRange(min, max, "0.05", rate -> {
            getMSDForRate(rate.toPlainString()).ifPresent(ratedMSD -> {
                if (msdFilter.test(normal, ratedMSD)) {
                    this.info.timeNotesWith(baseTiming.rated(rate));
                    action.accept(this, rate, ratedMSD);
                }
            });
        });
    }

    /**
     * For each rate 0.7 to 2.0 in 0.05 increments.
     *
     * @param filter The MSD Filter.
     * @param action The action to apply.
     */
    public void forEachRateFull(final BiPredicate<MSD, MSD> filter,
                                final RatedChartHandler action) {
        forEachRate("0.7", "2.0", filter, action);
    }

    /**
     * For each rate 0.7 to 1.5 in 0.05 increments.
     *
     * @param filter The MSD Filter.
     * @param action The action to apply.
     */
    public void forEachRateHalf(final BiPredicate<MSD, MSD> filter,
                                final RatedChartHandler action) {
        forEachRate("0.7", "1.5", filter, action);
    }

    /**
     * Handles a rated instance of a chart.
     */
    public static interface RatedChartHandler {
        void accept(CachedNoteInfo info, BigDecimal rate, MSD msd);
    }
}
