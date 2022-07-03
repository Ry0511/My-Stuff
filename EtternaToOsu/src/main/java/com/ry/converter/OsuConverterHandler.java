package com.ry.converter;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.util.MSDChart;
import com.ry.ffmpeg.AsyncFFMPEG;
import com.ry.ffmpeg.FFMPEG;
import com.ry.ffmpeg.FFMPEGUtils;
import com.ry.osu.builderRedone.HitObject;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.tracker.LoggingListener;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java class created on 24/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@CommonsLog
public class OsuConverterHandler<T extends OsuConverter> {

    /**
     * The underlying converter context which is used to create converted output
     * files.
     */
    private final T converterImpl;

    public void convert(File root, File output) throws IOException {
        converterImpl.streamConvertableFiles(root)
                .forEach(x -> handleProcessing(x, output));
    }

    private void handleProcessing(MSDChart chart, File output) {

        final EtternaFile eFile = chart.getInfo().getParent();
        final File songDir = converterImpl.getSongDir(chart, output);

        if (songDir.isDirectory() || songDir.mkdirs()) {
            final String audioFilename = eFile.getAudioFile()
                    .map(File::getAbsolutePath)
                    .map(x -> converterImpl.createAudioFile(chart, songDir, x))
                    .orElse(null);

            final String bgFilename = eFile.getBackgroundFile()
                    .map(File::getAbsolutePath)
                    .map(x -> converterImpl.createBackgroundFile(chart, songDir, x))
                    .orElse(null);

            converterImpl.createOsuFile(chart, songDir, audioFilename, bgFilename);

        } else {
            log.warn(String.format("Song Dir: %-32s doesn't exist, failed to "
                            + "be created, or is a file; Skipping file %s (Rate: %sx)",
                    output.toPath().relativize(songDir.toPath()).toString(),
                    chart.getInfo().getParent().getSmFile().getName(),
                    chart.getRate()
            ));
        }
    }

    public static void main(String[] args) {
        FFMPEG mpeg = FFMPEGUtils.getFfmpegFromPath().get();

        AsyncFFMPEG base = new AsyncFFMPEG(mpeg, 8);
        AsyncFFMPEG rated = new AsyncFFMPEG(mpeg, 16);

        AtomicBoolean inherited = new AtomicBoolean(false);
        OsuConverterHandler<OsuConverterImpl> handler = new OsuConverterHandler<>(new OsuConverterImpl(
                OsuConverterImpl.initCalculated(OsuConverterHandler::filter),
                new BasicChartFinaliser(
                        x -> x.setOverallDifficulty("10").setHpDrain("10"),
                        x -> x.setUnInherited(inherited.getAndSet(true)),
                        x -> x.runIf(OsuConverterHandler::isShortHold, () -> x.setType(HitType.HIT))
                ),
                base, rated
        ));

        OsuConverterImpl impl = (OsuConverterImpl) handler.getConverterImpl();
        LoggingListener ls = new LoggingListener();
        impl.getTracker().addAudioConversionListener(ls);
        impl.getTracker().addBackgroundListener(ls);
        
        try {
            Instant start = Instant.now();
            handler.convert(
                    new File("C:\\Games\\Etterna\\Songs\\Blos Pack 5"),
                    new File("G:\\Games\\osu!2\\Songs")
            );

            base.terminateAndWait(() -> {
            });
            rated.terminateAndWait(() -> {
            });

            Instant end = Instant.now();
            System.out.println("Time Taken: " + Duration.between(start, end).toMillis() + "ms");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean isShortHold(HitObject.HitObjectBuilder x) {
        if (x.getEndTime() == null || x.getType() != HitType.MANIA_HOLD) {
            return false;
        }
        var diff = x.getEndTime().subtract(x.getEndTime(), MathContext.DECIMAL64);
        return diff.compareTo(new BigDecimal("22.5")) <= 0;
    }

    private static boolean filter(MSD msd, MSDChart msdChart) {
        return true;
    }
}
