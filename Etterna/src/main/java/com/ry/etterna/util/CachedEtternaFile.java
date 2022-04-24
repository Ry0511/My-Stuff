package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaTiming;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@Setter(AccessLevel.PRIVATE)
@Deprecated
public class CachedEtternaFile {

    /**
     * The loaded Stepmania file.
     */
    private final EtternaFile smFile;

    /**
     * Cached all dance-single 4k charts.
     */
    private final List<CachedNoteInfo> notes = new ArrayList<>();

    /**
     * @param cacheDB Cache DB.
     * @param smFile Stepmania file.
     * @throws IOException  If the file cannot be parsed.
     * @throws SQLException If the queries fail.
     */
    public CachedEtternaFile(
            final CacheDB cacheDB,
            final File smFile) throws IOException, SQLException {
        this(cacheDB, new EtternaFile(smFile));
    }

    /**
     * @param cacheDB Cache DB.
     * @param etternaFile Processed Stepmania File.
     * @throws SQLException If the cache db cannot be queried.
     */
    public CachedEtternaFile(
            final CacheDB cacheDB,
            final EtternaFile etternaFile) throws SQLException {
        this.smFile = etternaFile;

        final EtternaTiming timing = etternaFile.getTimingInfo();
        for (final EtternaNoteInfo info : etternaFile.getNoteInfo()) {
            if (info.isDanceSingle()) {
                info.timeNotesWith(timing);
                info.queryStepsCache(cacheDB)
                        .ifPresent(cache -> notes.add(
                                new CachedNoteInfo(cache, info)
                        ));
            }
        }
    }
}
