package com.ry.redone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.etterna.util.EtternaIterator;
import com.ry.ffmpeg.FFMPEG;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

/**
 * Java class created on 25/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class Main {

    // TODO This is tooooooooooooooooooooooooooooooooooooooooooooooooooooo slow

    public static final MathContext C = MathContext.DECIMAL64;
    public static final BigDecimal DEVIATION_LIMIT = new BigDecimal("8");
    public static final BigDecimal MIN_DOWN_RATE_MSD = new BigDecimal("26.5");
    public static final BigDecimal MIN_MSD = new BigDecimal("22.5");
    public static final BigDecimal MAX_MSD = new BigDecimal("35.5");

    public static final BiPredicate<MSD, MSD> FILTER = (base, rated) -> {

        // 1.0 is not allowed
        if (base.equals(rated)) {
            return false;
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
    };

    public static final File CACHE_FILE = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    public static final File SONGS_DIR = new File("C:\\Games\\Etterna\\Songs");
    public static final File OUTPUT_DIR = new File("G:\\Games\\- - - Converts (29-04-2022)");


    public static void main(final String[] args) throws SQLException, IOException {
        final Instant start = Instant.now();
        final CacheDB db = new CacheDB(CACHE_FILE);

        final File[] packs = SONGS_DIR.listFiles();
        if (packs != null) {
            for (int i = 1; i < packs.length; ++i) {
                final ExecutorService service = Executors.newFixedThreadPool(4);
                final EtternaIterator iter = new EtternaIterator(packs[i]);
                iter.setFilter(EtternaFile::isStandard);

                //
                // For each file pass to handleNotes(FILE)
                //

                try {
                    service.shutdown();
                    service.awaitTermination(300, TimeUnit.HOURS);

                    final Instant end = Instant.now();
                    System.out.printf(
                            "Pack Conversion completed after %ss%n",
                            Duration.between(start, end).getSeconds()
                    );

                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        db.close();
        FFMPEG.INSTANCE.getExecutor().shutdown();
    }

    private static void handleNotes(final List<CachedNoteInfo> xs) {
        final EtternaFile file = xs.get(0).getEtternaFile();

        xs.get(0).getMSDForRate("1.0").ifPresent(msd -> {
            final ConvertableFile base = new ConvertableFile(
                    OUTPUT_DIR,
                    xs.get(0),
                    "1.0",
                    msd
            );

            if (base.getOsuFile().isFile()) {
                System.out.printf(
                        "[SKIP] File '%s' already exists...%n",
                        base.getOsuFile().getAbsolutePath()
                );
                return;
            }

            try {
                // Convert 1.0
                if (base.createSongDir()) {
                    base.createBackgroundFile();

                    final Process p = FFMPEG.INSTANCE.execAndWait(
                            base.getAudioConvertCommand()
                    );
                    System.out.printf(
                            "[INFO] '%s' exited with '%s'%n",
                            base.getAudioFile().getAbsolutePath(),
                            p.exitValue()
                    );

                    // Create .osu file
                    createNormal(base, xs);

                    // Queue rates 0.7 to 2.0 using default filter
                    createRates(xs);
                } else {
                    System.err.printf(
                            "[FAIL] Base directory '%s' not created!",
                            base.getSongDir().getAbsolutePath()
                    );
                }

                // Error print + skip
            } catch (final IOException | InterruptedException e) {
                e.printStackTrace();
                System.err.println(
                        "[SKIP] " + file.getSmFile().getAbsolutePath()
                );
            }
        });
    }

    private static void createNormal(
            final ConvertableFile base,
            final List<CachedNoteInfo> xs) throws IOException {

        final var v = base.asOsuBuilder().build();
        v.getTimingPoints()
                .stream()
                .skip(1)
                .forEach(x -> x.setUnInherited(false));
        base.createOsuFile(v.asOsuStr());


        for (int i = 1; i < xs.size(); ++i) {
            final CachedNoteInfo x = xs.get(i);
            var msd = x.getMSDForRate("1.0");

            if (msd.isPresent()) {
                final ConvertableFile f = new ConvertableFile(
                        OUTPUT_DIR, x, "1.0", msd.get()
                );

                final var m = f.asOsuBuilder().build();
                m.getTimingPoints()
                        .stream()
                        .skip(1)
                        .forEach(y -> y.setUnInherited(false));
                f.createOsuFile(m.asOsuStr());
            }

            System.gc();
        }
    }

    private static void createRates(final List<CachedNoteInfo> xs) {
        xs.forEach(file -> {
            file.forEachRateFull(FILTER, (info, rate, msd) -> {
                if (!rate.equals(BigDecimal.ONE)) {
                    final ConvertableFile f = new ConvertableFile(
                            OUTPUT_DIR,
                            info,
                            rate.toPlainString(),
                            msd
                    );

                    // Create rates
                    try {
                        // Creating .mp3
                        if (!f.getAudioFile().isFile()) {
                            FFMPEG.INSTANCE.execAndWait(f.getAudioConvertCommand());
                            System.gc();
                        }

                        // Creating .osu
                        final var v = f.asOsuBuilder().build();
                        v.getTimingPoints().stream()
                                .skip(1)
                                .forEach(x -> x.setUnInherited(false));
                        f.createOsuFile(v.asOsuStr());
                    } catch (final Exception e) {
                        e.printStackTrace();
                        System.err.println(
                                "[FAIL] Failed to Write File: "
                                        + f.getOsuFile().getAbsolutePath()
                        );
                    }
                }
            });
        });
    }
}
