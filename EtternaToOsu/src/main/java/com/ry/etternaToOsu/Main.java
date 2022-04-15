package com.ry.etternaToOsu;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.useful.StringUtils;
import com.ry.useful.property.SimpleStringProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java class created on 13/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class Main {

    private static final ExecutorService EXECUTOR_SERVICE
            = Executors.newFixedThreadPool(6);
    private static final File FILE_DIR = new File("C:\\Games\\Etterna\\Songs\\Jumpstream Peru 2");
    private static final File OUTPUT_DIR = new File("D:\\Program Files x86\\osu!\\Songs\\- - - - CONVERT PACKS ver(05-08-21)");

    public static void main(String[] args) throws Exception {
        final CacheDB db
                = new CacheDB(new File("C:\\Games\\Etterna\\Cache\\cache.db"));
        final String rate = "1.0";

        System.out.printf(
                "FILES: %s%nOUTPUT: %s%n",
                FILE_DIR.getAbsolutePath(),
                OUTPUT_DIR.getAbsolutePath()
        );

        FileUtils.iterateFiles(FILE_DIR, new SuffixFileFilter(".sm"), TrueFileFilter.TRUE).forEachRemaining(x -> {
            EXECUTOR_SERVICE.submit(() -> {
                try {
                    convertFile(OUTPUT_DIR, x, db, rate);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed: " + x.getAbsolutePath());
                }
            });
        });

        EXECUTOR_SERVICE.shutdown();
        EXECUTOR_SERVICE.awaitTermination(24, TimeUnit.HOURS);
    }

    private static void convertFile(final File output,
                                    final File smFile,
                                    final CacheDB db,
                                    final String rate) throws Exception {
        EtternaFile file = new EtternaFile(smFile);
        int diffID = 0;
        for (final EtternaNoteInfo info : file.getNoteInfo()) {
            if (info.isDanceSingle()) {
                info.timeNotesWith(file.getTimingInfo());

                final CacheStepsResult result
                        = db.getStepCacheFor(info.getChartKey4K()).orElse(null);

                if (result != null && result.getMSDForRate(rate).isPresent()) {
                    final EtternaConverter converter = new EtternaConverter(
                            file, info
                    );
                    converter.initDefaultMetadata(
                            result.getMSDForRate(rate).get(),
                            diffID
                    );
                    converter.setElem(OsuElement.CREATOR, "Overcomplicated Conversion Tool");

                    final File dest = new File(StringUtils.buildPath(
                            output,
                            file.getPackFolder().getName(),
                            file.getSongFolder().getName()
                    ));

                    if (dest.isDirectory() || dest.mkdirs()) {
                        final OsuFileStructure struct = OsuFileStructure.of(dest);

                        struct.setOsuFile(struct.appendBaseDir(String.format(
                                "[%s] (%s - %s).osu",
                                result.getMSDForRate(rate).get().getSkill(SkillSet.OVERALL),
                                rate,
                                file.getProperty(EtternaProperty.TITLE).asMutatingValue(SimpleStringProperty::getProcessed)
                                        .mutateValue(StringUtils::toFileName)
                                        .getValue()
                        )));
                        converter.setElem(OsuElement.TITLE, file.getPackFolder().getName());
                        converter.setElem(OsuElement.TITLE_UNICODE, file.getPackFolder().getName());
                        converter.convert(struct, rate);
                    }
                }
            }
            ++diffID;
        }
    }
}
