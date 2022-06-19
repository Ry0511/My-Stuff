package com.ry.ffmpeg;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * Java class created on 23/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class Task {

    /**
     * Process builder used to create live tasks.
     */
    @Getter(AccessLevel.PRIVATE)
    private final ProcessBuilder pb;

    private final String[] args;

    /**
     * Creates the task from the string arguments.
     *
     * @param args The tasks arguments.
     */
    public Task(final String... args) {
        this.pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        this.args = args;
    }

    /**
     * Starts the task and then immediately returns.
     *
     * @return The started task.
     * @throws IOException If an IO error occurs.
     */
    public Process start() throws IOException {
        return pb.start();
    }

    /**
     * Starts the task and waits for its exit code to become available.
     *
     * @return The potentially finished task.
     * @throws InterruptedException If while waiting, interrupted.
     * @throws IOException          If an IO error occurs.
     */
    public Process startAndWait() throws InterruptedException, IOException {
        final Process p = pb.start();

        final var in = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = in.readLine()) != null) {
            // Just consume the stream
        }
        p.waitFor();
        in.close();

        return p;
    }

    /**
     * @param p The process to create a writer for.
     * @return Buffered writer attached to the processes output stream.
     */
    public static BufferedWriter out(@NonNull final Process p) {
        return new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
    }

    /**
     * @param p The process to create a reader for.
     * @return Buffered reader attached to the provided processes input stream.
     */
    public static BufferedReader in(@NonNull final Process p) {
        return new BufferedReader(new InputStreamReader(p.getInputStream()));
    }
}
