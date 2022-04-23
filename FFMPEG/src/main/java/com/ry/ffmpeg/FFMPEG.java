package com.ry.ffmpeg;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

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
     * Static FFMPEG instance which utilises a work stealing pot.
     */
    public static final FFMPEG INSTANCE
            = new FFMPEG(Executors.newWorkStealingPool());

    /**
     * The ffmpeg command executor.
     */
    @Getter(AccessLevel.PRIVATE)
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

    /**
     * Executes the provided command under the currently stored ffmpeg header.
     *
     * @param args The ffmpeg command array.
     * @return Future of the currently executing task.
     */
    public Future<Process> exec(final String... args) {
        return executor.submit(() -> {
            final String[] cmd = new String[args.length + 1];
            cmd[0] = FFMPEGUtils.quote(getFfmpeg());
            System.arraycopy(args, 0, cmd, 1, args.length);
            final Task t = new Task(cmd);
            return t.startAndWait();
        });
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
