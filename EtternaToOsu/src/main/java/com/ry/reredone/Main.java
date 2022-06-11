package com.ry.reredone;

import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.ffmpeg.FFMPEG;
import com.ry.osu.builder.BuildableOsuFile;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Java class created on 20/05/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class Main {

    // todo Beatmap set & Beatmap id are not set producing broken files, and
    //  the 1.0x audio file is broken since its looking for 1.00x just a name
    //  issue.

    // todo this is a little bit faster however still takes 24s to convert 35
    //  audio files. The fastest approach I can think of would be to also
    //  asynchronously create the 1.0x files as well, however this has two
    //  main issues firstly this would increase the memory overhead (Should
    //  be fine since it takes a long time for me to get OFM errors),
    //  secondly we cant start the rated audio file conversions until the 1
    //  .0x audio file is created thus we need to block the submission of the
    //  audio tasks and have them wait of the 1.0x audio somehow.

    public static final MathContext C = MathContext.DECIMAL64;
    public static final BigDecimal DEVIATION_LIMIT = new BigDecimal("8");
    public static final BigDecimal MIN_DOWN_RATE_MSD = new BigDecimal("26.5");
    public static final BigDecimal MIN_MSD = new BigDecimal("22.5");
    public static final BigDecimal MAX_MSD = new BigDecimal("35.5");

    public static final File CACHE_FILE = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    public static final File SONGS_DIR = new File("C:\\Games\\Etterna\\Songs");
    //    public static final File OUTPUT_DIR = new File("G:\\Games\\osu!\\Songs\\- - - - Test Shit");
    public static final File OUTPUT_DIR = new File("G:\\Games\\osu!\\Songs\\- - - - Converts (24-05-2022)");

    public static void main(final String[] args) throws SQLException, IOException {
        final AsyncAudioService aas = new AsyncAudioService(
                Executors.newWorkStealingPool(16),
                new FFMPEG()
        );

        final CacheDB db = new CacheDB(CACHE_FILE);

        final Converter converter = new Converter(
                aas, db, SONGS_DIR,
                Main::msdFilter,
                Main::mutateOutput,
                Main::onAudioQueued,
                Main::onOsuFail
        );

        try {
            final Instant start = Instant.now();
            converter.start(
                    OUTPUT_DIR, Executors.newWorkStealingPool(8), 8,
                    () -> System.out.printf(
                            "[UPDATE] Completed: %-6s Active: %-6s Total: %-6s "
                                    + "Elapsed Time: %sms%n",
                            aas.getCompletedTasks(),
                            aas.getActiveTasks(),
                            aas.getTotalTasks(),
                            Duration.between(start, Instant.now()).toMillis()
                    ));

            aas.getEs().shutdown();
            while (!aas.getEs().awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.printf(
                        "[UPDATE] Completed: %-6s Active: %-6s Total: %-6s "
                                + "Elapsed Time: %sms%n",
                        aas.getCompletedTasks(),
                        aas.getActiveTasks(),
                        aas.getTotalTasks(),
                        Duration.between(start, Instant.now()).toMillis()
                );
            }
            System.out.printf("Time taken: %sms%n",
                    Duration.between(start, Instant.now()).toMillis()
            );
            System.out.printf(
                    "[UPDATE] Completed: %-4s Active: %-4s Total: %-4s%n",
                    aas.getCompletedTasks(),
                    aas.getActiveTasks(),
                    aas.getTotalTasks()
            );
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("[MALFORMED ROOT DIRECTORY] :: " + SONGS_DIR);
        }
    }

    private static boolean msdFilter(final MSD base, final MSD rated) {

        // 1.0 is always allowed
        if (base.equals(rated)) {
            return true;
        }

        final BigDecimal b = base.getSkill(SkillSet.OVERALL);
        final BigDecimal r = rated.getSkill(SkillSet.OVERALL);

        // Down rated
        final boolean filter;
        if (b.compareTo(r) > 0) {
            filter = b.compareTo(MIN_DOWN_RATE_MSD) >= 0;

            // Uprated
        } else {
            filter = r.subtract(b, C).compareTo(DEVIATION_LIMIT) <= 0;
        }

        // If filter true and in range
        return filter
                && r.compareTo(MIN_MSD) >= 0
                && r.compareTo(MAX_MSD) <= 0;
    }

    private static BuildableOsuFile mutateOutput(final BuildableOsuFile.BuildableOsuFileBuilder b,
                                                 final String rate,
                                                 final MSD msd,
                                                 final ConvertableFile srcFile) {
        // The excess timing points can be ignored only the first actually
        // matters
        final var v = b.build();
        v.getTimingPoints()
                .stream()
                .skip(1)
                .forEach(x -> x.setUnInherited(false));

        return v;
    }

    private static void onAudioQueued(final String smFileAbsPath,
                                      final File audioFile,
                                      final Future<Boolean> future) {

    }

    private static boolean onOsuFail(final CachedNoteInfo info,
                                     final ConvertableFile cf,
                                     final String rate,
                                     final IOException ex) {

        // Delete the potentially malformed files
        try {
            FileUtils.deleteDirectory(cf.getSongDir());
        } catch (final IOException ignored) {
            System.err.println("[DELETE FAILED] " + cf.getSongDir());
        }

        // Print error info
        System.err.printf(
                "[OSU CREATION FAIL]%n\tRate: %s%n\tCachedNoteInfo: " +
                        "%s%n\tConvertableFile: %s%n\t[IO-EXCEPTION]%n%s",
                rate, info.toString(), cf.toString(), ex.getMessage()
        );
        ex.printStackTrace();

        // Skip the rest of the pack
        return false;
    }
}
