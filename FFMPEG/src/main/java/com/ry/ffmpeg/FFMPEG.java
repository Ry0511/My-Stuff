package com.ry.ffmpeg;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Java class created on 19/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class FFMPEG {

    /**
     * The executable ffmpeg reference.
     */
    private final String executable;

    /**
     * If true, then when creating commands the executable will be encased in
     * double quotes.
     */
    private final boolean isQuoteFfmpeg;

    /**
     * @return The executable ffmpeg reference.
     * @see #isQuoteFfmpeg()
     */
    public String getExecutable() {
        if (isQuoteFfmpeg()) {
            return "\"" + executable + "\"";
        } else {
            return executable;
        }
    }

    /**
     * @param args The command for the process builder.
     * @return Default process builder with error redirected.
     */
    private static ProcessBuilder initProcess(final String[] args) {
        final ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        return pb;
    }

    /**
     * @param p The process to wrap a buffered reader for.
     * @return The process input stream wrapped in a buffered reader.
     */
    private static BufferedReader initOutputStream(final Process p) {
        return new BufferedReader(new InputStreamReader(p.getInputStream()));
    }

    /**
     * Executes the provided command, under the ffmpeg executable.
     *
     * @param cmd The command to run.
     * @return The process exit code.
     * @throws IOException          If any occur.
     * @throws InterruptedException If while waiting for the process to exit
     *                              normally the calling thread is interrupted.
     * @throws ExecutionException   If process was interrupted before finishing
     *                              and had to be cancelled.
     */
    public int exec(final String[] cmd) throws IOException, InterruptedException, ExecutionException {
        final ProcessBuilder pb = initProcess(
                Stream.concat(Stream.of(cmd), Arrays.stream(cmd))
                        .toArray(String[]::new)
        );

        final Process p = pb.start();
        try (final BufferedReader out = initOutputStream(p)) {
            while ((out.readLine()) != null) {
                // Just flushing the stream
            }
            try {
                p.onExit().get();
            } catch (final InterruptedException ex) {
                return p.destroyForcibly().waitFor();
            }
        }

        return p.exitValue();
    }

    /**
     * Executes the provided command, under the ffmpeg executable.
     *
     * @param cmd The command to run.
     * @return The process exit code.
     * @throws IOException          If any occur.
     * @throws InterruptedException If while waiting for the process to exit
     *                              normally the calling thread is interrupted.
     * @throws ExecutionException   If process was interrupted before finishing
     *                              and had to be cancelled.
     */
    public int exec(final IOCommand cmd) throws IOException, ExecutionException, InterruptedException {
        final var listener = cmd.getListener();
        final ProcessBuilder pb = initProcess(cmd.build(getExecutable()));

        // Start process
        final Process p = pb.start();
        if (listener != null) {
            listener.onStart(cmd);
        }

        // Read the output
        try (final BufferedReader out = initOutputStream(p)) {
            // Not sure if this optimisation is really needed I never tested it.
            // It just seemed like an easy fix.
            if (listener != null) {
                String line;
                while ((line = out.readLine()) != null) {
                    listener.onMessage(cmd, line);
                }
            } else {
                while (out.readLine() != null) {
                    // Flush output
                }
            }
        }

        // When finished invoke listener
        try {
            p.onExit().thenApply(x -> {
                if (listener != null) {
                    listener.onComplete(cmd, x.exitValue());
                }
                return null;
            }).get();
        } catch (final InterruptedException ex) {
            return p.destroyForcibly().waitFor();
        }

        return p.exitValue();
    }

    /**
     * Runs the provided io command asynchronously.
     *
     * @param cmd The command the run.
     * @return Future representing the async task. Upon get, if null is returned
     * then an exception occurred, else if not null then the execution was
     * handled normally.
     */
    public CompletableFuture<Integer> execAsync(final IOCommand cmd) {
        // Never knew about this API
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exec(cmd);
            } catch (final IOException | ExecutionException | InterruptedException e) {
                return null;
            }
        });
    }
}
