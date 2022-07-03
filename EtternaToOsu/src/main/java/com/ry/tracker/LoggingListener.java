package com.ry.tracker;

import com.ry.etterna.util.MSDChart;
import lombok.Data;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Java class created on 30/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class LoggingListener implements AudioConversionListener, BackgroundConversionListener {

    private static final int GC_TICKER = 100;

    private int mode = 0;
    private Instant start = Instant.now();
    private AtomicInteger total = new AtomicInteger();
    private AtomicInteger gcTicker = new AtomicInteger();
    private int totalComplete = 0;
    private int totalMalformed = 0;
    private HashSet<String> malformedSet = new HashSet<>();

    public Stream<String> streamMalformedAudio() {
        return malformedSet.stream().filter(x -> x.startsWith("AudioFail?"));
    }

    public Stream<String> streamMalformedBackground() {
        return malformedSet.stream().filter(x -> x.startsWith("BackgroundFail?"));
    }

    public void addToTotal() {
        total.getAndIncrement();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Audio file.
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onSubmitted() {
        addToTotal();
    }

    @Override
    public void onMalformed(final MSDChart chart, final String[] ffmpegCommand, final int exit) {
        runAsync(() -> {
            ++totalMalformed;
            malformedSet.add(String.format(
                    "AudioFail?=(Chart '%s';%n\tCommand: '%s';%n\tExit: %s)",
                    chart.getEtternaFile().getSmFile().getAbsolutePath(),
                    Arrays.toString(ffmpegCommand),
                    exit
            ));
        });
    }

    @Override
    public void onComplete(final MSDChart chart, final String[] ffmpegCommand) {
        runAsync(() -> {
            ++totalComplete;

            switch (getMode()) {
                case 0 -> printState();
                case 1 -> System.out.printf(
                        "[%.2fs][AUDIO] %s completed.%n",
                        Duration.between(start, Instant.now()).toMillis() / 1000D,
                        ffmpegCommand[ffmpegCommand.length - 1]
                );
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Background file
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onMalformed(final MSDChart chart, final int exit, final String[] ffmpegCommand) {
        runAsync(() -> {
            ++totalMalformed;
            malformedSet.add(String.format(
                    "BackgroundFail?=(Chart '%s';%n\tCommand: '%s';%n\tExit: %s)",
                    chart.getEtternaFile().getSmFile().getAbsolutePath(),
                    Arrays.toString(ffmpegCommand),
                    exit
            ));
        });
    }

    @Override
    public void onComplete(final MSDChart chart, final String backgroundFile, final File outputFile) {
        runAsync(() -> {
            ++totalComplete;

            switch (getMode()) {
                case 0 -> printState();
                case 1 -> System.out.printf(
                        "[%.2fs][BACKGROUND] Bg file %s created.%n",
                        Duration.between(start, Instant.now()).toMillis() / 1000D,
                        outputFile.getAbsolutePath()
                );
            }
        });
    }

    private synchronized void runAsync(final Runnable action) {
        action.run();
        // Every 100 calls attempt to garbage collect.
        if (gcTicker.getAndIncrement() >= GC_TICKER) {
            gcTicker.getAndSet(0);
            System.gc();
        }
    }

    public void printState() {
        final int total = getTotal().get();
        System.out.printf(
                "[%.2f][STATS] Total Queued: %s | Total Complete: %s | Total Malformed: %s | %.2f\r",
                Duration.between(start, Instant.now()).toMillis() / 1000D,
                total,
                getTotalComplete(),
                getTotalMalformed(),
                ((double) getTotalComplete() / (double) total) * 100
        );
    }
}
