package com.ry.tracker;

import com.ry.etterna.util.MSDChart;
import lombok.Data;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Java class created on 30/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class LoggingListener implements AudioConversionListener, BackgroundConversionListener {

    private Instant start = Instant.now();
    private int totalComplete = 0;
    private int totalMalformed = 0;
    private HashSet<String> malformedSet = new HashSet<>();

    public Stream<String> streamMalformedAudio() {
        return malformedSet.stream().filter(x -> x.startsWith("AudioFail?"));
    }

    public Stream<String> streamMalformedBackground() {
        return malformedSet.stream().filter(x -> x.startsWith("BackgroundFail?"));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Audio file.
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void onMalformed(final MSDChart chart, final String[] ffmpegCommand, final int exit) {
        ++totalMalformed;
        malformedSet.add(String.format(
                "AudioFail?=(Chart '%s';%n\tCommand: '%s';%n\tExit: %s)",
                chart.getEtternaFile().getSmFile().getAbsolutePath(),
                Arrays.toString(ffmpegCommand),
                exit
        ));
    }

    @Override
    public synchronized void onComplete(final MSDChart chart, final String[] ffmpegCommand) {
        ++totalComplete;
        System.out.printf(
                "[%.2fs][AUDIO] %s completed.%n",
                Duration.between(start, Instant.now()).toMillis() / 1000D,
                ffmpegCommand[ffmpegCommand.length - 1]
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Background file
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void onMalformed(final MSDChart chart, final int exit, final String[] ffmpegCommand) {
        ++totalMalformed;
        malformedSet.add(String.format(
                "BackgroundFail?=(Chart '%s';%n\tCommand: '%s';%n\tExit: %s)",
                chart.getEtternaFile().getSmFile().getAbsolutePath(),
                Arrays.toString(ffmpegCommand),
                exit
        ));
    }

    @Override
    public synchronized void onComplete(final MSDChart chart, final String backgroundFile, final File outputFile) {
        ++totalComplete;
        System.out.printf(
                "[%.2fs][BACKGROUND] Bg file %s created.%n",
                Duration.between(start, Instant.now()).toMillis() / 1000D,
                outputFile.getAbsolutePath()
        );
    }
}
