package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.useful.StreamUtils;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@CommonsLog
public class CachedNoteInfo implements MinaCalculated {

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
                        final CacheStepsResult cache = info.queryStepsCache(db).orElse(null);

                        if (cache != null) {
                            xs.add(new CachedNoteInfo(cache, info));
                        } else {
                            log.warn("Could not find Cached MSD Info for File: " + x.getSmFile());
                        }

                        // Skip on fail
                    } catch (final SQLException e) {
                        log.error("File: " + x.getSmFile() + "; Couldn't be processed; reason: " + e + "; Skipping this chart.");
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
    // Streaming rates
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a stream populated with this chart and its MSD values for all
     * rates in the provided range.
     *
     * @param min The min rate.
     * @param max The max rate.
     */
    @Override
    public Stream<MSDChart> streamRateInRange(final BigDecimal min, final BigDecimal max) {

        if (min.compareTo(new BigDecimal("0.7")) < 0) {
            throw new IllegalStateException("Rate " + min + " unsupported.");
        }

        if (max.compareTo(new BigDecimal("2.0")) > 0) {
            throw new IllegalStateException("Rate " + max + " unsupported.");
        }

        return StreamUtils.createStreamLazily(supplyChart(min.toPlainString(), max.toPlainString()));
    }

    /**
     * @param min Min (Start).
     * @param max Max (End).
     */
    private Supplier<MSDChart> supplyChart(final String min,
                                           final String max) {
        final AtomicReference<BigDecimal> i = new AtomicReference<>(new BigDecimal(min));
        final BigDecimal increment = new BigDecimal("0.05");
        final BigDecimal ma = new BigDecimal(max);
        return () -> {

            if (i.get().compareTo(ma) <= 0) {
                final String rate = i.get().toPlainString();
                final Optional<MSD> msd = getMSDForRate(rate);

                // Update to the next
                i.set(i.get().add(increment, MathContext.DECIMAL64));

                return MSDChart.of(rate, msd.orElse(null), getInfo());
            } else {
                return null;
            }
        };
    }
}
