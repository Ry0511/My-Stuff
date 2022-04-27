package com.ry.redone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.etterna.util.EtternaIterator;
import com.ry.ffmpeg.FFMPEG;
import com.ry.osu.builder.HitObject;
import com.ry.osu.builder.TimingPoint;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
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

    // TODO This works however is unreasonably slow, optimise this cleanly.

    public static final BigDecimal DEVIATION_LIMIT = new BigDecimal("8");
    public static final BigDecimal MIN_MSD = new BigDecimal("22.5");
    public static final BigDecimal MAX_MSD = new BigDecimal("35.5");

    public static final BiPredicate<MSD, MSD> FILTER = (base, rated) -> {

        // 1.0 is not allowed
        if (base.equals(rated)) {
            return false;
        }

        final BigDecimal b = base.getSkill(SkillSet.OVERALL);
        final BigDecimal r = rated.getSkill(SkillSet.OVERALL);

        // From 1.0 try not to rate to upwards passed +k MSD
        final BigDecimal result = b.subtract(r, MathContext.DECIMAL64);
        final BigDecimal back
                = result.signum() == -1 ? result.negate() : result;

        // If deviation from Origin is less than the expected and in range
        return back.compareTo(DEVIATION_LIMIT) <= 0
                && r.compareTo(MIN_MSD) >= 0
                && r.compareTo(MAX_MSD) <= 0;
    };

    public static final ExecutorService ASYNC_AUDIO_TASKS
            = Executors.newFixedThreadPool(6);

    public static final File CACHE_FILE = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    public static final File SONGS_DIR = new File("C:\\Games\\Etterna\\Songs");
    public static final File OUTPUT_DIR = new File("C:\\Users\\-Ry\\Desktop\\Output-dir");


    public static void main(final String[] args) throws SQLException {
        CacheDB db = new CacheDB(CACHE_FILE);
        EtternaIterator iter = new EtternaIterator(SONGS_DIR);
        iter.setFilter(EtternaFile::isStandard);

        iter.forEachCached(db, cachedInfo -> {

            cachedInfo.getMSDForRate("1.0").ifPresent(normalMSD -> {
                convertFile(map(cachedInfo, "1.0", normalMSD));

                cachedInfo.forEachRateFull(Main.FILTER, (info, rate, msd) -> {
                    if (!rate.equals(new BigDecimal("1.0"))) {
                        convertFile(map(info, rate.toPlainString(), msd));
                    }
                });
            });
        });

        try {
            ASYNC_AUDIO_TASKS.shutdown();
            ASYNC_AUDIO_TASKS.awaitTermination(300, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ConvertableFile map(final CachedNoteInfo info,
                                      final String rate,
                                      final MSD msd) {
        return new ConvertableFile(OUTPUT_DIR, info, rate, msd);
    }

    public static void convertFile(final ConvertableFile file) {
        final File audio = file.getAudioFile();
        final boolean isNormalRate = file.getRate().equals("1.0");

        if (file.createSongDir()) {

            // Convert audio
            if (!audio.isFile()) {
                if (isNormalRate) {
                    file.createNormalAudio(FFMPEG.INSTANCE);
                } else {
                    if (file.getBaseRateAudio().isFile()) {
                        ASYNC_AUDIO_TASKS.submit(() -> {
                            try {
                                FFMPEG.INSTANCE.execAndWait(
                                        file.asyncRatedAudio()
                                );
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }

            // Convert BG
            if (isNormalRate && !file.getBgFile().isFile()) {
                file.createBackgroundFile();
            }

            // Create osu file
            var osuBuilder = file.asOsuBuilder().build();
            osuBuilder.getTimingPoints().stream()
                    .skip(1)
                    .forEach(x -> x.setUnInherited(false));

            try {
                file.createOsuFile(osuBuilder.asOsuStr());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
