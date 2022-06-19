package com.ry.reredone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.osu.builder.BuildableOsuFile;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Java class created on 13/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class DefaultConverterController implements Converter.ConversionListener {

    /**
     * The maximum allowed deviation between the 1.0 MSD and a rated MSD value.
     */
    private final BigDecimal maxDeviation;

    /**
     * The minimum MSD to allow for Down-rates to occur. I.e., if 25 then the
     * MSD must be greater than or equal to 25 in order to produce 0.95,...,0.70
     * rates.
     */
    private final BigDecimal minDownRateMSD;

    /**
     * The minimum MSD allowed uprating, that is if the 1.1 MSD is less than
     * this, then 1.1 will not be created.
     */
    private final BigDecimal minUpRateMSD;

    /**
     * The maximum MSD allowed when uprating, that is, if the 1.1 MSD is greater
     * than this, then 1.1 will not be created.
     */
    private final BigDecimal maxUpRateMSD;

    /**
     * The place to create/place convert packs.
     */
    private final Path outputDestination;

    /**
     * Converts the provided pack dumping the results to the output destination.
     * Note that only Standard etterna files are converted see {@link
     * EtternaFile#isStandard()} for specifics.
     *
     * @param aas The async audio service, note that this should be terminated
     * when this method returns.
     * @param db The cache db to probe for MSD info, this will stay open and is
     * not closed by this method.
     * @param pack The pack to convert.
     * @param onAwait The action to perform whilst waiting for the process to
     * conclude.
     * @return True if the process has concluded naturally.
     */
    public boolean convert(final AsyncAudioService aas,
                           final CacheDB db,
                           final File pack,
                           final int nSongThreads,
                           final Runnable onAwait) {

        final ExecutorService service = Executors.newWorkStealingPool(nSongThreads);
        final Converter converter = new Converter(aas, db, pack, this, this, this, this);

        try {
            converter.start(getOutputDestination().toFile(), service, nSongThreads, onAwait);
            return service.isTerminated();
        } catch (final IOException | InterruptedException e) {
            if (!service.isShutdown()) {
                System.err.printf(
                        "[ERROR] %s; cancelling %s tasks...%n",
                        e.getMessage(),
                        service.shutdownNow().size()
                );
            }
        }

        return false;
    }

    /**
     * @param b The builder attached to the convertable file to mutate.
     * @param rate The rate of the Osu File.
     * @param msd The MSD for the subject rate.
     * @param target The actual difficulty being converted.
     */
    @Override
    public BuildableOsuFile accept(final BuildableOsuFile.BuildableOsuFileBuilder b,
                                   final String rate,
                                   final MSD msd,
                                   final ConvertableFile target) {

        final BuildableOsuFile built = b.build();
        built.getTimingPoints().stream()
                .skip(1)
                .forEach(x -> x.setUnInherited(true));

        return built;
    }

    /**
     * @param smFile The absolute path to the original Step-mania file that was
     * converted, String is passed to reduce GC issues.
     * @param audioFile The audio file which has been queued.
     * @param task Future of the queued task.
     */
    @Override
    public void accept(final String smFile,
                       final File audioFile,
                       final Future<Boolean> task) {

    }

    /**
     * @param cni The etterna file which failed to produce a complete Osu file
     * for.
     * @param cf The convertable file which failed.
     * @param rate The target rate that failed.
     * @param ex The literal exception raised.
     * @return {@code true} if the program should continue producing rates for
     * the subject chart. If false then it should just skip this pack.
     */
    @Override
    public boolean accept(final CachedNoteInfo cni,
                          final ConvertableFile cf,
                          final String rate,
                          final IOException ex) {
        System.err.printf(
                "The chart '%s' failed; reason: '%s'; Output Dir: '%s', Rate: '%s'; Skipping this chart%n",
                cni.getEtternaFile().getSmFile().getName(),
                ex.getMessage(),
                cf.getSongDir(),
                rate
        );
        return false;
    }

    /**
     * Applies the MSD Filter.
     *
     * @param base The base 1.0 MSD Value.
     * @param other The other MSD Value to test.
     */
    @Override
    public boolean test(final MSD base, final MSD other) {

        // 1.0 is always allowed
        if (base.equals(other)) {
            return true;
        }

        final BigDecimal baseOverall = base.getSkill(SkillSet.OVERALL);
        final BigDecimal ratedOverall = other.getSkill(SkillSet.OVERALL);

        // Down rated
        final boolean filter;
        if (baseOverall.compareTo(ratedOverall) > 0) {
            filter = baseOverall.compareTo(getMinDownRateMSD()) >= 0;

            // Uprated
        } else {
            filter = ratedOverall.subtract(baseOverall, MathContext.DECIMAL64)
                    .compareTo(getMaxDeviation()) <= 0;
        }

        // If filter true and in range
        return filter
                && ratedOverall.compareTo(getMinUpRateMSD()) >= 0
                && ratedOverall.compareTo(getMaxUpRateMSD()) <= 0;
    }
}
