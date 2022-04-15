package com.ry.etterna;

import com.ry.etterna.db.CacheDB;
import com.ry.etterna.db.CacheStepsResult;
import com.ry.etterna.note.EtternaNoteInfo;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Java class created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class ReaderTester {

    private static final File BROKEN_FILE = new File("G:\\Dev\\Projects\\IntelliJ\\FunctionalUtils\\Etterna\\src\\test\\java\\com\\ry\\etterna\\broken");
    private static final File OUT_PUT
            = new File("G:\\Dev\\Projects\\IntelliJ\\FunctionalUtils\\Etterna\\src\\test\\java\\com\\ry\\etterna\\debug-out");

    private static final File CACHE_DB = new File("C:\\Games\\Etterna\\Cache\\cache.db");
    private static final File SONGS_DIR = new File("C:\\Games\\Etterna\\Songs");
    private static final File TEST_FILE = new File("D:\\Program Files x86\\Etterna\\Songs\\10Dollar Dump Dump Revolutions 2\\Ehhen Doyadosu Tengujiman (35)\\Ehhen Doyadosu Tengujiman .sm");
    private static CacheDB CACHE_DB_INSTANCE;

    static {
        try {
            System.setOut(new PrintStream(OUT_PUT.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            CACHE_DB_INSTANCE = new CacheDB(CACHE_DB);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static int failed = 0;

    public static void main(String[] args) throws IOException {
        FileUtils.streamFiles(SONGS_DIR, true, "sm", "ssc")
                .forEach(ReaderTester::readFile);

//        readFile(new File("C:\\Games\\Etterna\\Songs\\00.interesting files collection 2\\ANTI_THE_HOLiC\\ah.sm"));
        System.err.println("Files Failed: " + failed);
    }

    private static void readFile(final File file) {
        try {
            if (!file.getParentFile().getParentFile().getParentFile().getName().equalsIgnoreCase("Songs"))
                return;

            final EtternaFile etternaFile = new EtternaFile(file);
            final File blank = new File("Couldn't Find");
            System.out.printf(
                    "[FILE: %s]%nAudio: '%s'%nBG   : '%s'%n",
                    file.getAbsolutePath(),
                    etternaFile.getAudioFile().orElse(blank),
                    etternaFile.getBackgroundFile().orElse(blank)
            );
            System.out.println();

            CACHE_DB_INSTANCE.close();
            // Read fail
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println(file.getAbsolutePath());
            System.exit(-1);
        }
    }
}
