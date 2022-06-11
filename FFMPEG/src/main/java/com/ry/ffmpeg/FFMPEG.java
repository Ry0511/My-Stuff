package com.ry.ffmpeg;

import lombok.Data;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Java class created on 23/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public final class FFMPEG {

    /**
     * Static FFMPEG instance which utilises a work stealing pot with an
     * unbounded level of parallelism.
     */
    public static final FFMPEG INSTANCE
            = new FFMPEG(Executors.newWorkStealingPool(4));

    /**
     * The ffmpeg command executor.
     */
    private final ExecutorService executor;

    /**
     * The ffmpeg path for execution by default this is "ffmpeg" and assumes the
     * user has it on their PATH.
     */
    private String ffmpeg = "ffmpeg";

    /**
     * Constructs a ffmpeg instance from the base executor.
     *
     * @param ex The executor.
     */
    public FFMPEG(final ExecutorService ex) {
        this.executor = ex;
    }

    public FFMPEG() {
        this.executor = null;
    }

    /**
     * Creates a CLI Task which operates under with some FFMPEG instance.
     *
     * @param mpeg The ffmpeg reference.
     * @param args The command arguments.
     * @return New task which is [ffmpeg, args[0], args[k]]
     */
    private static Task create(final String mpeg,
                               final String[] args) {
        final String[] cmd = new String[args.length + 1];
        cmd[0] = FFMPEGUtils.quote(mpeg);
        System.arraycopy(args, 0, cmd, 1, args.length);

        return new Task(cmd);
    }

    /**
     * Executes the provided command asynchronously.
     *
     * @param args The ffmpeg command array.
     * @return Future of the currently executing task.
     * @throws UnsupportedOperationException If the executor is null.
     */
    public Future<Process> exec(final String... args) {
        if (executor == null) throw new UnsupportedOperationException();
        return executor.submit(() -> create(getFfmpeg(), args).startAndWait());
    }

    /**
     * Executes the provided command and blocks until the command returns.
     *
     * @param args The command to run.
     * @return The completed process.
     * @throws IOException          If any occur.
     * @throws InterruptedException If while waiting, interrupted.
     */
    public Process execAndWait(final String... args)
            throws IOException, InterruptedException {
        return create(getFfmpeg(), args).startAndWait();
    }

    /**
     * @param path The new ffmpeg path.
     */
    public synchronized void setFfmpeg(final String path) {
        this.ffmpeg = path;
    }

    /**
     * Terminates this FFMPEG instance.
     *
     * @param timeout The maximum time to wait for currently active tasks.
     * @param unit Timescale of the timeout (MS, Days, etc)
     * @return {@code true} if the termination has been completed, else {@code
     * false} if some tasks are still running.
     * @throws InterruptedException If while waiting, interrupted.
     */
    public boolean close(final long timeout,
                         final TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }
}
