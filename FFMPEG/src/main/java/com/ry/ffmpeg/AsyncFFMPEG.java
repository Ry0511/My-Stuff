package com.ry.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Java class created on 24/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@AllArgsConstructor
public class AsyncFFMPEG {

    /**
     * The maximum time to wait in milliseconds for the executor to shut down
     * properly. This wait should be chained and periodically checked.
     */
    private static final int WAIT_TIME = 1500;

    /**
     * The ffmpeg reference used to execute commands.
     */
    private final FFMPEG ffmpeg;

    /**
     * The service used to run async tasks.
     */
    private final ExecutorService service;

    /**
     * @param ffmpeg The ffmpeg instance to execute under.
     * @param parallelism The target parallelism.
     */
    public AsyncFFMPEG(final FFMPEG ffmpeg, final int parallelism) {
        this.ffmpeg = ffmpeg;
        this.service = Executors.newWorkStealingPool(parallelism);
    }

    /**
     * Executes the string args on the ffmpeg instance asynchronously.
     *
     * @param cmd The aforementioned command.
     * @param onFinish Invoked on the resulting exit code.
     * @param onError Invoked if an exception is raised before the task exits.
     * @return Future representing this task.
     */
    public Future<?> exec(final String[] cmd,
                          final Consumer<Integer> onFinish,
                          final Consumer<Exception> onError) {
        return service.submit(() -> {
            try {
                onFinish.accept(ffmpeg.exec(cmd));
            } catch (final Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Executes the IOCommand on the ffmpeg instance.
     *
     * @param cmd The aforementioned command.
     * @param onError Invoked if an exception is raised before the task exits.
     * @return Future representing this task.
     */
    public Future<?> exec(final IOCommand cmd, final Consumer<Exception> onError) {
        return service.submit(() -> {
            try {
                ffmpeg.exec(cmd);
            } catch (final Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Executes the provided action with the executor service.
     *
     * @param action The action to perform.
     * @return Future of the action.
     */
    public Future<?> exec(final Consumer<FFMPEG> action) {
        return service.submit(() -> action.accept(getFfmpeg()));
    }

    /**
     * Executes the provided action with the executor service.
     *
     * @param action The action to perform.
     * @param <T> The result of the action.
     * @return Future representing the scheduled task.
     */
    public <T> Future<T> exec(final Function<FFMPEG, T> action) {
        return service.submit(() -> action.apply(getFfmpeg()));
    }

    /**
     * Invokes the shutdown of the underlying executor, awaiting its exit if
     * possible.
     *
     * @param action The action to be performed every {@link #WAIT_TIME}ms
     * whilst waiting for the complete shutdown of the service. This can be null
     * if no action is desired.
     * @throws InterruptedException Iff while waiting, interrupted.
     */
    public void terminateAndWait(final Runnable action) throws InterruptedException {
        this.service.shutdown();

        while (!this.service.awaitTermination(WAIT_TIME, TimeUnit.MILLISECONDS)) {
            if (action != null) {
                action.run();
            }
        }
    }
}
