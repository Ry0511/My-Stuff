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

    // todo this is a little bit faster however still takes 24s to convert 35
    //  audio files.

    public static final MathContext C = MathContext.DECIMAL64;
    public static final BigDecimal DEVIATION_LIMIT = new BigDecimal("8");
    public static final BigDecimal MIN_DOWN_RATE_MSD = new BigDecimal("26.5");
    public static final BigDecimal MIN_MSD = new BigDecimal("22.5");
    public static final BigDecimal MAX_MSD = new BigDecimal("35.5");

    public static final File CACHE_FILE = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    public static final File SONGS_DIR = new File("C:\\Games\\Etterna\\Songs");
    public static final File OUTPUT_DIR = new File("G:\\Games\\osu!2\\Songs\\Songs\\- - - - Test Shit");

    public static void main(final String[] args) throws SQLException, InterruptedException {
        final AsyncAudioService aas = new AsyncAudioService(
                Executors.newWorkStealingPool(8),
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
            Instant start = Instant.now();
            converter.start(OUTPUT_DIR, xs -> {
                final var x = xs.get(0);
                System.out.printf("[SKIP]\t%-64s", x.getEtternaFile().getSmFile());
            });

            aas.getEs().shutdown();
            aas.getEs().awaitTermination(6000, TimeUnit.HOURS);
            System.out.printf("Time taken: %sms%n",
                    Duration.between(start, Instant.now()).toMillis()
            );
        } catch (final IOException e) {
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
        System.out.printf("[QUEUED] %-64s%n", audioFile.getAbsolutePath());
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
