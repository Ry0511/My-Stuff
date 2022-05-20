package com.ry.reredone;

import com.ry.ffmpeg.FFMPEG;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Synchronized;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java class created on 19/05/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@Getter(AccessLevel.PRIVATE)
public class AsyncAudioService {

    /**
     * The service used to initiate parallelism with.
     */
    @Getter(AccessLevel.PUBLIC)
    private final ExecutorService es;

    /**
     * The ffmpeg command executor.
     */
    @Getter(AccessLevel.PUBLIC)
    private final FFMPEG ffmpeg;

    /**
     * Hash set of started/submitted conversions. This uses absolute paths to
     * ensure that each entry is 'unique' this just removes the possibility of
     * duplicated work.
     */
    private final HashSet<String> startedConversions = new HashSet<>();

    /**
     * The number of tasks currently started and running.
     */
    private final AtomicInteger activeTasks = new AtomicInteger();

    /**
     * The number of tasks that have concluded/finished.
     */
    private final AtomicInteger completedTasks = new AtomicInteger();

    /**
     * Submits the chart and preps execution.
     *
     * @param chart The chart to convert the audio file for.
     * @return Empty if the task was not submitted, else an optional of the
     * future state of the task in where True is successful termination.
     */
    @Synchronized
    public Optional<Future<Boolean>> submit(final ConvertableFile chart) {
        final File f = chart.getAudioFile();
        final String absPath = f.getAbsolutePath();

        // Convert if it is unique
        if (startedConversions.add(absPath)) {
            final var task = es.submit(() -> {
                try {
                    activeTasks.getAndIncrement();
                    final Process p = ffmpeg.execAndWait(chart.getAudioConvertCommand());
                    activeTasks.getAndDecrement();
                    completedTasks.getAndIncrement();
                    return p.exitValue() == 0 && (f.isFile() || f.exists());
                } catch (final Exception ex) {
                    return false;
                }
            });

            return Optional.of(task);
        }

        return Optional.empty();
    }

    /**
     * @return Number of tasks currently running.
     */
    public int getActiveTasks() {
        return this.activeTasks.get();
    }

    /**
     * @return Number of tasks that have been completed.
     */
    public int getCompletedTasks() {
        return this.completedTasks.get();
    }

    /**
     * @return The total number of tasks submitted.
     */
    public int getTotalTasks() {
        return this.startedConversions.size();
    }

    /**
     * Adds the completed task to this service.
     *
     * @param name The completed task to add.
     * @return {@code true} if the task has been added, {@code false} if it
     * already existed.
     */
    public boolean addCompletedTask(final String name) {
        return this.startedConversions.add(name);
    }
}
