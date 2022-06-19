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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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
    private File rootDir;

    /**
     * @param service Default conversion tool utilises parallelism for the Rates
     * as this just speeds the process up an absurd amount.
     * @param db Open CacheDB connection to allow MSD probing.
     * @param root Root directory to search for packs.
     * @param msdFilter Rate filter, that is, Param 0 is 1.0 MSD and Param 1 is
     * the current Rate MSD.
     * @param osuBuilderMutator Just before writing the .osu files this is
     * invoked on the base builder, this allows the editing of the output file.
     * @param queuedAudioHandler When an audio file is queued for creation this
     * is invoked to perhaps cancel or probe the result.
     * @param onOsuFileFail When writing the output file fails this is invoked.
     */
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

    ///////////////////////////////////////////////////////////////////////////
    // Various different parallelisms of the conversion, tried a few
    // approaches some are 'barbaric' others are more specialised to a need
    // which lets them outperform others.
    ///////////////////////////////////////////////////////////////////////////

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
                        final List<Future<Boolean>> xs = handleAllRates(difficulties, outputDir);

                        for (final Future<Boolean> x : xs) {
                            try {
                                x.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        onSkip.accept(difficulties);
                    }
                });
    }

    /**
     * Greater parallelism performance overall for the conversion. However, this
     * method has a much higher CPU and Memory usage as the entirety of the
     * files (All songs) are kept in memory for the 1.00x rate this means that
     * Garbage-Collection cannot run whilst the rates for the 1.00x are still in
     * memory/waiting so depending on what Executor service is provided
     * Garbage-Collection may not be able to run at all.
     *
     * @param outputDir The directory to place the output files.
     * @param es The executor service to use to parallelize the 1.00x rates.
     * @param onSkip Invoked when a file is skipped.
     * @throws Exception If any occur.
     */
    public void start(final File outputDir,
                      final ExecutorService es,
                      final Consumer<List<CachedNoteInfo>> onSkip) throws Exception {

        final List<Callable<List<Future<Boolean>>>> rates = Collections.synchronizedList(new ArrayList<>());
        final EtternaIterator iter = new EtternaIterator(rootDir);
        iter.setFilter(EtternaFile::isStandard);

        iter.getEtternaStream()
                .map(x -> CachedNoteInfo.from(x, db))
                .filter(xs -> !xs.isEmpty())
                .forEach(difficulties -> {
                    // Rates rely on the 1.0 Audio file, so it needs to exist
                    // before the rates get submitted.

                    es.submit(() -> {
                        if (createBaseStructure(difficulties.get(0), outputDir)) {
                            rates.add(() -> handleAllRates(difficulties, outputDir));

                        } else {
                            onSkip.accept(difficulties);
                        }
                    });
                });

        es.shutdown();
        System.out.println("[WAIT FOR 1.00X]");
        while (!es.awaitTermination(30, TimeUnit.SECONDS)) ;

        System.out.println("[INITIATE RATES]");
        for (final var rate : rates) rate.call();
    }

    /**
     * Memory efficient variant of start, this approach is more for quick
     * cleanup after each Song but still offering the appropriate parallelism
     * for each song.
     * <p>
     * Note this variant is the most optimal in terms of speed and memory
     * usage.
     *
     * @param outDir The output directory.
     * @param asyncBaseRateService Service used to initiate the 1.0x rates.
     * @param memTicker This is the maximum number of Songs allowed in memory at
     * any one time. You can multiply this by 27 (0.7 -> 2.0 : +0.05) to assert
     * the total number of tasks for each file (Maximum).
     * @param onAwait Invoked every 'k' seconds while awaiting the termination
     * of the conversion process. Used mainly as a Ping->Pong.
     * @throws InterruptedException If interrupted whilst waiting for the
     *                              process to finish.
     * @throws IOException          If the output current root directory is
     *                              invalid.
     */
    public void start(final File outDir,
                      final ExecutorService asyncBaseRateService,
                      final int memTicker,
                      final Runnable onAwait) throws IOException, InterruptedException {
        final EtternaIterator iter = new EtternaIterator(this.rootDir);
        iter.setFilter(EtternaFile::isStandard);

        final Semaphore semaphore = new Semaphore(memTicker);
        iter.getEtternaStream()
                .map(x -> CachedNoteInfo.from(x, db))
                .filter(xs -> !xs.isEmpty())
                .forEach(diffs -> {
                    // Rates rely on the 1.0 Audio file, so it needs to exist
                    // before the rates get submitted.
                    asyncBaseRateService.submit(() -> {
                        try {
                            semaphore.acquire();
                            if (createBaseStructure(diffs.get(0), outDir)) {
                                System.out.printf(
                                        "[CONVERT] %s%n",
                                        diffs.get(0).getEtternaFile().getSmFile()
                                );
                                for (final var v : handleAllRates(diffs, outDir))
                                    v.get();
                            }
                            System.gc();
                            semaphore.release();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                });

        asyncBaseRateService.shutdown();
        while (!asyncBaseRateService.awaitTermination(30, TimeUnit.SECONDS))
            onAwait.run();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Below is specific to creating the output files.
    ///////////////////////////////////////////////////////////////////////////

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
                        outputDir, difficulty, "1.00", baseMSD.get()
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
    private List<Future<Boolean>> handleAllRates(final List<CachedNoteInfo> difficulties,
                                                 final File outputDir) {

        final List<Future<Boolean>> tasks = new ArrayList<>();
        final AtomicBoolean skip = new AtomicBoolean(false);
        for (final CachedNoteInfo diff : difficulties) {
            if (skip.get()) return tasks;

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
                        tasks.add(x);
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

        return tasks;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Interfaces used to allow the caller to invoke calls to change
    // the final output without affecting the overall control flow.
    ///////////////////////////////////////////////////////////////////////////

    public static interface ConversionListener extends
            OsuBuilderMutator,
            OsuFileFailHandler,
            QueuedAudioHandler,
            BiPredicate<MSD, MSD> {
        // Just a wrapper interface.
    }

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
