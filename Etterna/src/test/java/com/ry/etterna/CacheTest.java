package com.ry.etterna;

import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.etterna.util.EtternaIterator;

import java.io.File;
import java.sql.SQLException;

/**
 * Java class created on 23/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class CacheTest {

    public static final File CACHE
            = new File("C:\\Games\\Etterna\\Cache\\cache.db");

    public static final File SONGS_DIR
            = new File("C:\\Games\\Etterna\\Songs");

    public static void main(String[] args) throws SQLException {
        final CacheDB db = new CacheDB(CACHE);
        final EtternaIterator iter = new EtternaIterator(SONGS_DIR);
        iter.setFilter(EtternaFile::isStandard);

        iter.forEachCached(db, cached -> {
            
        });
    }
}
