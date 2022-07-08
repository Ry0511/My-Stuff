package com.ry.etterna.util;

import com.ry.etterna.db.CacheDB;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Java class created on 21/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class CachedFiles {

    private static final File CACHE_DB = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    private static final File SONG_DIR = new File("C:\\Games\\Etterna\\Songs");

    public static void main(String[] args) {

        try {
            final EtternaIterator iter = new EtternaIterator(SONG_DIR);
            final CacheDB db = new CacheDB(CACHE_DB);

            iter.getEtternaStream().forEach(x -> {
//                System.out.printf("[INFO] File: %-128s; Pack Structure: %-6s; Offset: %-8s%n",
//                        x.getSmFile(),
//                        x.hasPackStructure(),
//                        x.getOffset().map(BigDecimal::toPlainString).orElse("NaN")
//                );

                // Cached MSD
//                CachedNoteInfo.from(x, db).stream()
//                        .map(MinaCalculated::streamRateInRange)
//                        .forEach(c -> {
//                            c.limit(3).forEach(cc -> {
//                                System.out.printf(
//                                        "[INFO] Chart: %-48s | MSD: %-78s | Rate: %s%n",
//                                        cc.getInfo().getParent().getSmFile().getName(), cc.getMsd(), cc.getRate()
//                                );
//                            });
//                            System.out.println();
//                        });

                //Calculated MSD
                CalculatedNoteInfo.from(x).stream()
                        .peek(y -> y.setScoreGoal(0.93f))
                        .map(MinaCalculated::streamRateInRange)
                        .forEach(c -> c.limit(3).forEach(cc -> {
                            System.out.printf(
                                    "[INFO] Chart: %-48s | MSD: %-78s | Rate: %s%n",
                                    cc.getInfo().getParent().getSmFile().getName(), cc.getMsd(), cc.getRate()
                            );
                        }));
                System.out.println();

            });

            main(args);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
