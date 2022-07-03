package com.ry.converter;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.etterna.util.CalculatedNoteInfo;
import com.ry.etterna.util.EtternaIterator;
import com.ry.etterna.util.MSDChart;
import com.ry.etterna.util.MinaCalculated;
import com.ry.ffmpeg.AsyncFFMPEG;
import com.ry.ffmpeg.FFMPEGUtils;
import com.ry.ffmpeg.IOCommand;
import com.ry.tracker.AudioConversionListener;
import com.ry.tracker.BackgroundConversionListener;
import com.ry.tracker.CompletionSwitch;
import com.ry.tracker.SimpleConversionTracker;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Java class created on 25/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Value
@CommonsLog
@Getter(AccessLevel.PRIVATE)
public class OsuConverterImpl implements OsuConverter {

    //todo Perhaps an overhaul of the entire converter system. This works but
    // I don't like how its implemented, since there was no real design/structure.

    private static final String BASE_RATE_AUDIO = "1.00-Audio.mp4";

    /**
     * Maps some etterna note info to a stream of mina-calculated charts.
     */
    Function<EtternaNoteInfo, Stream<MSDChart>> mapToCalculatedChart;

    /**
     * Takes some partially complete Template file builder and maps the result
     */
    ChartFinaliser chartFinaliser;

    /**
     * Async ffmpeg handler which will handle execution of base 1.0 rates.
     */
    @Getter
    AsyncFFMPEG baseAudioService;

    /**
     * Async ffmpeg handler which will handle execution of any rate not 1.0.
     */
    @Getter
    AsyncFFMPEG ratedAudioService;

    /**
     * Map consisting of Absolute audio file paths, and a base 1.0 completion
     * switch. The rates, k -> v, only start when the switch is marked complete.
     * This is not synchronised, since access is only ever done on a single
     * thread basis.
     */
    Map<String, CompletionSwitch> baseRateAudioMap = new HashMap<>();

    /**
     * List of all audio tasks started.
     */
    List<Future<?>> audioConversionTasks = new ArrayList<>();

    /**
     * Tracker object which is pinged with updates about any conversion as its
     * happening.
     */
    @Getter
    SimpleConversionTracker tracker = new SimpleConversionTracker();

    ///////////////////////////////////////////////////////////////////////////
    // Factory initialisation methods. Covers loading MSD data from the Cache.db
    // and inline calculations; or a mix of the two in where if error, then
    // calculated. All factory methods provide the base MSD Filter options.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initialises from charts from the filter predicate and a mapping
     * function.
     *
     * @param filter The MSD filter, in where argument 0 is the base 1.0 MSD and
     * argument 1 is some rated MSD; note 1.0 can be the rated MSD.
     * @param mapToCalculated Maps a regular Etterna Note Info chart to some
     * calculated MSD.
     * @return Mapping function that will provide a stream of MSDCharts all of
     * whom are accepted and timed.
     */
    public static Function<EtternaNoteInfo, Stream<MSDChart>> init(final BiPredicate<MSD, MSDChart> filter,
                                                                   final Function<EtternaNoteInfo, MinaCalculated> mapToCalculated) {
        final AtomicReference<MSD> base = new AtomicReference<>();
        return (x -> Stream.of(x)
                .filter(EtternaNoteInfo::isDanceSingle)
                .map(mapToCalculated)
                .peek(y -> base.getAndSet(y.getMSDForRate("1.0").orElseThrow()))
                .filter(y -> base.get() != null)
                .flatMap(MinaCalculated::streamRateInRange)
                .filter(Objects::nonNull)
                .filter(MSDChart::isMSDPresent)
                // Is 1.0 or filter is true
                .filter(y -> y.isBaseRate() || filter.test(base.get(), y))
        );
    }

    /**
     * Initialises the converter to use only inline calculated charts with the
     * provided filter.
     *
     * @param filter MSD Filter.
     * @return Mapping function that will provide a stream of MSDCharts.
     * @see #init(BiPredicate, Function)
     */
    public static Function<EtternaNoteInfo, Stream<MSDChart>> initCalculated(final BiPredicate<MSD, MSDChart> filter) {
        return init(filter, CalculatedNoteInfo::new);
    }

    /**
     * Initialises the converter to use only Cached MSD charts with the provided
     * filter.
     *
     * @param filter MSD Filter.
     * @param db The database to look through for cached MSD values.
     * @return Mapping function that will provide a stream of MSDCharts.
     * @see #init(BiPredicate, Function)
     */
    public static Function<EtternaNoteInfo, Stream<MSDChart>> initCached(final BiPredicate<MSD, MSDChart> filter, final CacheDB db) {
        return init(filter, chart -> {
            try {
                return db.getStepCacheFor(chart.getChartKey4K())
                        .map(x -> new CachedNoteInfo(x, chart))
                        .orElse(null);
            } catch (final SQLException e) {
                return null;
            }
        });
    }

    /**
     * Initialises the converter to use only inline calculated charts with the
     * provided filter.
     *
     * @param filter MSD Filter.
     * @return Mapping function that will provide a stream of MSDCharts.
     * @see #init(BiPredicate, Function)
     */
    public static Function<EtternaNoteInfo, Stream<MSDChart>> initEither(final BiPredicate<MSD, MSDChart> filter,
                                                                         final CacheDB db) {

        // Uses cache unless error/missing.
        return init(filter, noteInfo -> {
            try {
                return db.getStepCacheFor(noteInfo.getChartKey4K())
                        .map(y -> (MinaCalculated) new CachedNoteInfo(y, noteInfo))
                        .orElse(new CalculatedNoteInfo(noteInfo));
            } catch (final SQLException e) {
                return new CalculatedNoteInfo(noteInfo);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Actual class boilerplate.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates an Etterna Stream of all files which are convertable to an osu!
     * file.
     *
     * @param root The root directory to search for Etterna Files.
     * @return Stream of all possible and valid convertable etterna files.
     * @throws IOException Iff the root directory is cannot be traversed.
     */
    @Override
    public Stream<MSDChart> streamConvertableFiles(final File root) throws IOException {
        final EtternaIterator iter = new EtternaIterator(root);

        return iter.getEtternaStream()
                .filter(x -> x.getAudioFile().isPresent())
                .filter(x -> x.getOffset().isPresent())
                .map(EtternaFile::getNoteInfo)
                .flatMap(List::stream)
                .flatMap(mapToCalculatedChart);
    }

    /**
     * Gets and perhaps creates an output song directory to place the output
     * files into.
     *
     * @param chart The chart to get the Song directory for.
     * @param outputDir The root output directory.
     * @return A new, or old, existing directory.
     */
    @Override
    public File getSongDir(final MSDChart chart, final File outputDir) {
        final EtternaFile file = chart.getInfo().getParent();

        // 'Out/Pack/Song/...'
        if (file.hasPackStructure()) {
            return new File(outputDir.getAbsolutePath()
                    + "/" + file.getPackFolder().getName()
                    + "/" + file.getSongFolder().getName()
            );

            // 'Out/Song'
        } else {
            return new File(outputDir.getAbsolutePath()
                    + "/"
                    + file.getSongFolder().getName()
            );
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // The threading here is important and so a blocking system is probably
    // a requirement.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates in the provided output directory, the converted .osu file.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param audioName The name of the audio file; or if none exists, null.
     * @param bgName The name of the background file; or if none exists, null.
     */
    @Override
    public void createOsuFile(final MSDChart chart,
                              final File songDir,
                              final String audioName,
                              final String bgName) {
        final String fileName = chartFinaliser.getChartFilename(chart);
        final File osuFile = new File(songDir.getAbsolutePath() + "/" + fileName);

        if (!osuFile.isFile()) {
            try {
                Files.writeString(osuFile.toPath(), chartFinaliser.mapToTemplate(
                        chart,
                        audioName == null ? "" : audioName,
                        bgName == null ? "" : bgName
                ).compile());
            } catch (final IOException ex) {
                log.error("Failed to write to file: " + osuFile, ex);
            }
        }
    }

    /**
     * Creates in the provided output directory, the audio file for the chart.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param audioFile The absolute path to the audio file to convert.
     * @return The name of the audio file; this will be passed onwards to {@link
     * #createOsuFile(MSDChart, File, String, String)}
     */
    @Override
    public String createAudioFile(final MSDChart chart,
                                  final File songDir,
                                  final String audioFile) {

        this.baseRateAudioMap.putIfAbsent(audioFile, new CompletionSwitch());
        final CompletionSwitch cSwitch = baseRateAudioMap.get(audioFile);

        final EtternaFile eFile = chart.getInfo().getParent();
        assert eFile.getOffset().isPresent() : "Offset doesn't exist: " + eFile.getSmFile().getName();
        final String audioName = chart.getRate() + "-Audio.mp4";

        // If rate is 1.0
        if (chart.isBaseRate()) {

            final IOCommand cmd = FFMPEGUtils.delayAudio(
                    eFile.getOffset().get(),
                    audioFile,
                    songDir.getAbsolutePath() + "/" + audioName
            );

            // EXECUTION ON DIFFERENT THREAD
            cmd.setListener((CompletionListener) (exec, exit) -> {
                if (exit == 0) {
                    cSwitch.setComplete(true);
                    getTracker().forEachAudioListener(ls -> ls.onComplete(
                            chart, cmd.build("ffmpeg")
                    ));

                } else {
                    log.warn(String.format(
                            "Base 1.0 audio conversion exited with: %s; Cancelling conversion of ['%s' to '%s']",
                            exit, eFile.getSmFile().getAbsolutePath(),
                            songDir.getAbsolutePath()
                    ));

                    getTracker().forEachAudioListener(ls -> ls.onMalformed(
                            chart, cmd.build("ffmpeg"), exit
                    ));
                    cSwitch.setCancelled(true);
                }
            });

            // EXEC ON DIFFERENT THREAD
            getTracker().forEachAudioListener(AudioConversionListener::onSubmitted);
            withAudioList(xs -> xs.add(baseAudioService.exec(cmd, log::error)));

            // Queues the submission of rates to the rated audio service.
        } else {
            final IOCommand cmd = FFMPEGUtils.rateAudio(
                    songDir.getAbsolutePath() + "/" + BASE_RATE_AUDIO,
                    chart.getRate(),
                    false,
                    songDir.getAbsolutePath() + "/" + audioName
            );

            // EXECUTES ON A DIFFERENT THREAD
            cmd.setListener((CompletionListener) (ignored, exit) -> {
                if (exit != 0) {
                    log.warn(String.format(
                            "Rated audio conversion terminated with exit '%s' "
                                    + "this could mean that the output is malformed/broken. "
                                    + "Please assess '%s' converted to '%s [%sx]' manually.",
                            exit, chart.getInfo().getParent().getSmFile().getAbsolutePath(),
                            songDir.getAbsolutePath(), chart.getRate()
                    ));

                    getTracker().forEachAudioListener(ls -> ls.onMalformed(
                            chart, cmd.build("ffmpeg"), exit
                    ));
                } else {
                    getTracker().forEachAudioListener(ls -> ls.onComplete(
                            chart, cmd.build("ffmpeg")
                    ));
                }
            });

            // SUBMISSION ON CALLER THREAD (EXECUTION ON DIFFERENT THREAD)
            cSwitch.runOnComplete(() -> {
                withAudioList(xs -> {
                    getTracker().forEachAudioListener(AudioConversionListener::onSubmitted);
                    xs.add(ratedAudioService.exec(cmd, log::error));
                });
            });
        }

        return audioName;
    }

    /**
     * Creates in the provided output directory, the background image for the
     * chart.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param backgroundFile The absolute path to the background file to
     * convert.
     * @return The name of the background file; this will be passed onwards to
     * {@link #createOsuFile(MSDChart, File, String, String)}.
     */
    @Override
    public String createBackgroundFile(final MSDChart chart,
                                       final File songDir,
                                       final String backgroundFile) {

        final File outputFile = new File(songDir.getAbsolutePath() + "/BG.jpg");

        if (!outputFile.isFile()) {

            final IOCommand cmd = FFMPEGUtils.compressImage(
                    backgroundFile, songDir.getAbsolutePath() + "/BG"
            );
            int exit = 0;
            try {
                exit = baseAudioService.getFfmpeg().exec(cmd);
            } catch (final IOException | ExecutionException | InterruptedException e) {
                log.error(e);
            }

            if (exit != 0) {
                log.warn(String.format(
                        "Conversion of file '%s' had non-zero exit '%s'",
                        outputFile.getAbsolutePath(), exit
                ));
                final int finalExit = exit;
                getTracker().forEachBackgroundListener(xs -> xs.onMalformed(
                        chart, finalExit, cmd.build("ffmpeg")
                ));
            } else {
                getTracker().forEachBackgroundListener(BackgroundConversionListener::onSubmitted);
                getTracker().forEachBackgroundListener(xs -> xs.onComplete(
                        chart, backgroundFile, outputFile
                ));
            }
        }

        return outputFile.getName();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods used to update internal data.
    ///////////////////////////////////////////////////////////////////////////

    public synchronized void withAudioList(final Consumer<List<Future<?>>> action) {
        action.accept(this.audioConversionTasks);
    }
}
