package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaTiming;
import com.ry.ffmpeg.FFMPEGUtils;
import lombok.Data;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Predicate;


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
     * The minimum rate value.
     */
    private static final BigDecimal MIN_RATE = new BigDecimal("0.70", C);

    /**
     * Maximum rate value.
     */
    private static final BigDecimal MAX_RATE = new BigDecimal("2.00", C);

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
                        System.err.println("[SQL ERROR] " + x.getSmFile());
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

    /**
     * @param destPath The output destination path, this will always have the
     * .jpg suffix.
     * @return Future of the task or {@code null} if the background file doesn't
     * exist.
     */
    public Future<Process> convertImage(final String destPath) {
        final Optional<File> opt = getEtternaFile().getBackgroundFile();

        return opt.map(file -> FFMPEGUtils.compressImage(
                file.getAbsolutePath(),
                destPath
        )).orElse(null);
    }

    /**
     * Creates and starts an audio conversion task that will take the default
     * audio file and produce a new file at the provided destination path with
     * the required Delay.
     *
     * @param destinationPath The file path to create for the new audio file.
     * @return Future of the task or {@code null} if the Audio file doesn't
     * exist.
     */
    public Future<Process> convertAudio(final String destinationPath) {
        final Optional<File> optAudio = getInfo().getParent().getAudioFile();
        final Optional<BigDecimal> offset = getInfo().getParent().getOffset();

        if (optAudio.isPresent() && offset.isPresent()) {
            return FFMPEGUtils.delayAudio(
                    offset.get(),
                    optAudio.get().getAbsolutePath(),
                    destinationPath
            );
        }

        return null;
    }

    /**
     * For each rate 0.70, 0.75, ..., 2.0 apply the given action.
     *
     * @param action The action to apply for every rate.
     */
    public void forEachRate(final RatedChartHandler action) {
        final EtternaTiming normal = getInfo().getParent().getTimingInfo();
        BigDecimal v = MIN_RATE;

        while (v.compareTo(MAX_RATE) <= 0) {
            final BigDecimal clone = v;
            getMSDForRate(v.toString()).ifPresent(msd -> {
                getInfo().timeNotesWith(normal.rated(clone));
                action.accept(this, clone, msd);
            });

            v = v.add(HALF_RATE, MathContext.DECIMAL64);
        }
    }

    /**
     * Functionally the same as {@link #forEachRate(RatedChartHandler)} however
     * this one will only update the Info if the given predicate yields true.
     *
     * @param action The action to apply for every rate.
     */
    public void forEachRate(final Predicate<MSD> condition,
                            final RatedChartHandler action) {
        final EtternaTiming normal = getInfo().getParent().getTimingInfo();
        BigDecimal v = MIN_RATE;

        while (v.compareTo(MAX_RATE) <= 0) {
            final BigDecimal clone = v;
            getMSDForRate(v.toString()).ifPresent(msd -> {
                if (condition.test(msd)) {
                    getInfo().timeNotesWith(normal.rated(clone));
                    action.accept(this, clone, msd);
                }
            });

            v = v.add(HALF_RATE, MathContext.DECIMAL64);
        }
    }

    /**
     * Handles a rated instance of a chart.
     */
    public static interface RatedChartHandler {
        void accept(CachedNoteInfo info, BigDecimal rate, MSD msd);
    }
}
