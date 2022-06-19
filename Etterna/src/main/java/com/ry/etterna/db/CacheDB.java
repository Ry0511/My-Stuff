package com.ry.etterna.db;

import com.ry.useful.old.database.SQLiteDB;
import lombok.NonNull;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Java class created on 11/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class CacheDB extends SQLiteDB {

    /**
     * The base SQL Query for extracting step info from a chart key.
     */
    private static final String STEP_INFO_QUERY
            = "SELECT * FROM steps WHERE CHARTKEY = ?";

    /**
     * Step info from chart key query.
     */
    private final PreparedStatement stepInfoQuery;

    /**
     * @param cacheDb Etterna Cache.db file.
     * @throws SQLException Iff the database is invalid/corrupted/load failed.
     */
    public CacheDB(@NonNull File cacheDb) throws SQLException {
        super(cacheDb);
        stepInfoQuery = getDbConnection().prepareStatement(STEP_INFO_QUERY);
    }

    /**
     * Queries for the cached step data using the provided chart key as the
     * identity.
     *
     * @param chartKey The key to look for.
     * @return Optional of the found results.
     * @throws SQLException Iff querying fails for some reason.
     */
    public Optional<CacheStepsResult> getStepCacheFor(
            @NonNull final String chartKey) throws SQLException {
        synchronized (stepInfoQuery) {
            stepInfoQuery.setString(1, chartKey);
            final List<CacheStepsResult> results = query(
                    stepInfoQuery,
                    CacheStepsResult.class
            );

            if (results == null || results.isEmpty()) {
                return Optional.empty();
            } else {
                // It is possible to have more than 1, but we can ignore that.
                return Optional.of(results.get(0));
            }
        }
    }

    /**
     * Closes the database connection.
     */
    @Override
    public void close() throws SQLException {
        stepInfoQuery.close();
        super.close();
    }
}
