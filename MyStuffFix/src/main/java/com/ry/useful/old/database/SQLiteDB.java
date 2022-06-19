package com.ry.useful.old.database;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class SQLiteDB {

    /**
     * The sqlite database file.
     */
    private final File database;

    /**
     * The connection to the sqlite database.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Connection dbConnection;

    /**
     * Constructs the DB from the base sqlite file.
     *
     * @param sqliteDB SQLite db file.
     * @throws SQLException If any occur whilst opening a database connection.
     */
    public SQLiteDB(@NonNull final File sqliteDB) throws SQLException {
        this.database = sqliteDB;
        dbConnection = DriverManager
                .getConnection("jdbc:sqlite:" + sqliteDB.getAbsolutePath());
    }

    /**
     * @param action The action to perform.
     * @param <R> The type of the result from the action.
     * @return The result of the action.
     */
    public <R> R withConnection(final Function<Connection, R> action) {
        return action.apply(dbConnection);
    }

    /**
     * Runs the provided statement and wraps the results with provided target
     * class.
     *
     * @param stmt The statement to run.
     * @param target The target class to create.
     * @param <V> The type of the results' handler to create.
     * @return If there are no results from the query then {@code null} is
     * returned. However, if there is any number of results then a List of 'V'
     * is returned, this can be Singleton.
     * @throws SQLException If the query produces one.
     * @throws Error        If the provided Class is not setup for query
     *                      injection.
     */
    public <V> List<V> query(final PreparedStatement stmt,
                             final Class<V> target) throws SQLException {
        final ResultSet results = stmt.executeQuery();

        // Return instantly on empty
        if (!results.next()) {
            return null;
        }

        // Collect all matches
        final List<V> xs = new ArrayList<>();
        do {
            xs.add(SQLiteDBUtils.initTarget(target, results));
        } while (results.next());

        return xs;
    }

    /**
     * Closes the database connection.
     */
    public void close() throws SQLException {
        dbConnection.close();
    }
}
