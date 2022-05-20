package com.ry.reredone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.etterna.util.EtternaIterator;
import com.ry.osu.builder.BuildableOsuFile;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Java class created on 19/05/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@Getter(AccessLevel.PRIVATE)
public class Converter {

    /**
     * Filter to apply to all files, this dictates whether the subject file
     * should be converted or not.
     */
    private final BiPredicate<MSD, MSD> rateFilter;

    /**
     * Async audio converter used to offload the bulk audio conversion tasks
     * which are there will be many, many, many...
     */
    @Getter
    private final AsyncAudioService service;

    /**
     * Cache database used to find files
     */
    @Getter
    private final CacheDB db;

    /**
     * Mutator applied to the osu builder when creating files.
     */
    private final OsuBuilderMutator osuBuilderMutator;

    /**
     * Handler called whenever any audio file is queued for creation.
     */
    private final QueuedAudioHandler onAudioQueuedHandler;

    /**
     * Handler called whenever any SM/Osu file fails to convert raising an
     * exception.
     */
    private final OsuFileFailHandler onOsuFileFail;

    /**
     * The root directory containing packs to convert, semantically the files to
     * convert should be in the hierarchy: 'Songs > Pack > Song' though this
     * structure can be overridden by supplying your own Filter in the start
     * method.
     */
    @Getter
    private final File rootDir;

    public Converter(final AsyncAudioService service,
                     final CacheDB db,
                     final File root,
                     final BiPredicate<MSD, MSD> msdFilter,
                     final OsuBuilderMutator osuBuilderMutator,
                     final QueuedAudioHandler queuedAudioHandler,
                     final OsuFileFailHandler onOsuFileFail) {
        this.db = db;
        this.service = service;
        this.rootDir = root;
        this.rateFilter = msdFilter;
        this.osuBuilderMutator = osuBuilderMutator;
        this.onAudioQueuedHandler = queuedAudioHandler;
        this.onOsuFileFail = onOsuFileFail;
    }

    /**
     * @param outputDir The directory to place the converted pack/s.
     * @param onSkip Called when the subject list of difficulties will not be
     * converted due to them being improper/unsafe to do so.
     * @throws IOException If the Pack/Songs Directory fails to load.
     */
    public void start(final File outputDir,
                      final Consumer<List<CachedNoteInfo>> onSkip) throws IOException {
        final EtternaIterator iter = new EtternaIterator(rootDir);
        // The condition for Pack Structure might be obsolete now
        iter.setFilter(EtternaFile::isStandard);

        iter.getEtternaStream()
                .map(x -> CachedNoteInfo.from(x, db))
                .filter(xs -> !xs.isEmpty())
                .forEach(difficulties -> {
                    // Rates rely on the 1.0 Audio file, so it needs to exist
                    // before the rates get submitted.

                    if (createBaseStructure(difficulties.get(0), outputDir)) {
                        handleAllRates(difficulties, outputDir);
                    } else {
                        onSkip.accept(difficulties);
                    }
                });
    }

    /**
     * Creates the base file structure, that is, the 1.0 audio file, the
     * background file, and the Output directory structure will be created.
     *
     * @param difficulty Any difficulty from a single file.
     * @param outputDir The directory to output to.
     * @return {@code true} if the structure was created successfully. {@code
     * false} if not.
     */
    private boolean createBaseStructure(final CachedNoteInfo difficulty,
                                        final File outputDir) {
        final Optional<MSD> baseMSD = difficulty.getMSDForRate("1.0");

        try {
            if (baseMSD.isPresent()) {
                final ConvertableFile f = new ConvertableFile(
                        outputDir, difficulty, "1.0", baseMSD.get()
                );

                // Create file structure and bg file
                if (f.createSongDir()) {
                    f.createBackgroundFile();

                    // Create audio file
                    final String[] args = f.getAudioConvertCommand();
                    service.getFfmpeg().execAndWait(args);

                    // Only add if its completed
                    if (f.getAudioFile().isFile()) {
                        this.service.addCompletedTask(f.getAudioFile().getAbsolutePath());
                        return true;
                    }
                }
            }

            // This will really shouldn't happen
        } catch (final IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Default exit is to skip
        return false;
    }

    /**
     * Converts all rates.
     *
     * @param difficulties The difficulties to rate.
     * @param outputDir The directory to place the rates.
     */
    private void handleAllRates(final List<CachedNoteInfo> difficulties,
                                final File outputDir) {

        final AtomicBoolean skip = new AtomicBoolean(false);
        for (final CachedNoteInfo diff : difficulties) {
            if (skip.get()) return;

            diff.forEachRateFull(this.rateFilter, (info, rate, msd) -> {
                if (skip.get()) return;

                final ConvertableFile f = new ConvertableFile(
                        outputDir, info, rate.toPlainString(), msd
                );

                // Queue Audio (The string is provided since the File object
                // has a strong reference to the Etterna File container which
                // would/may prevent GC if given out and stored)
                if (!f.isNormalRate()) {
                    this.service.submit(f).ifPresent(x -> {
                        this.onAudioQueuedHandler.accept(
                                info.getEtternaFile().getSmFile().getAbsolutePath(),
                                f.getAudioFile(),
                                x
                        );
                    });
                }

                // Create .Osu
                final var s = this.osuBuilderMutator.accept(
                        f.asOsuBuilder(),
                        rate.toPlainString(),
                        msd,
                        f
                );
                try {
                    f.createOsuFile(s.asOsuStr());
                } catch (final IOException e) {
                    skip.getAndSet(onOsuFileFail.accept(
                            info, f, rate.toPlainString(), e
                    ));
                }
            });
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Interfaces used to allow the caller to invoke calls to change
    // the final output without affecting the overall control flow.
    ///////////////////////////////////////////////////////////////////////////

    public static interface OsuBuilderMutator {
        /**
         * @param b The builder attached to the convertable file to mutate.
         * @param rate The rate of the Osu File.
         * @param msd The MSD for the subject rate.
         * @param target The actual difficulty being converted.
         */
        BuildableOsuFile accept(BuildableOsuFile.BuildableOsuFileBuilder b,
                                String rate,
                                MSD msd,
                                ConvertableFile target);
    }

    public static interface QueuedAudioHandler {
        /**
         * @param smFile The absolute path to the original Step-mania file that
         * was converted, String is passed to reduce GC issues.
         * @param audioFile The audio file which has been queued.
         * @param task Future of the queued task.
         */
        void accept(String smFile, File audioFile, Future<Boolean> task);
    }

    public static interface OsuFileFailHandler {

        /**
         * @param cni The etterna file which failed to produce a complete Osu
         * file for.
         * @param cf The convertable file which failed.
         * @param rate The target rate that failed.
         * @param ex The literal exception raised.
         * @return {@code true} if the program should continue producing rates
         * for the subject chart. If false then it should just skip this pack.
         */
        boolean accept(CachedNoteInfo cni,
                       ConvertableFile cf,
                       String rate,
                       IOException ex);
    }
}
