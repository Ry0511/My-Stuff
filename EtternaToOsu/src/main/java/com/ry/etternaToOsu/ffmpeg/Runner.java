package com.ry.etternaToOsu.ffmpeg;

import com.ry.useful.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Java class created on 13/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Value
public class Runner {

    /**
     * The command to execute.
     */
    String[] command;

    /**
     * Called whilst an active task is populating its output stream.
     */
    Consumer<String> onLineFeed;

    /**
     * Used to create this task and execute on the client.
     */
    @Getter(AccessLevel.PRIVATE)
    ProcessBuilder pb;

    /**
     * Entity containing the latest executed/executing task, that is, if empty
     * then no task has been run, else if not then a task has run or is
     * running.
     */
    Entity<Process> latestTask = Entity.empty();

    public Runner(@NonNull final String[] command,
                  @NonNull final Consumer<String> onLineFeed) {
        this.command = command;
        this.onLineFeed = onLineFeed;
        this.pb = new ProcessBuilder(command);
        this.pb.redirectErrorStream(true);
    }

    /**
     * Starts the task and waits for termination.
     *
     * @param waitTime The maximum time to wait for termination.
     * @return {@code true} if the task has finished, else iff not then {@code
     * false} is returned.
     * @throws IOException          Iff one occurs whilst creating the task.
     * @throws InterruptedException If while waiting, interrupted.
     */
    public synchronized boolean start(final long waitTime)
            throws IOException, InterruptedException {
        System.out.println("[INFO] Running Task: " + taskToString());
        System.out.println();
        final Process task = pb.start();
        latestTask.setValue(task);

        // Read input
        new Thread(() -> ioHandle(task.getInputStream()), "IO-Handle").start();

        // Wait until done
        return task.waitFor(waitTime, TimeUnit.MILLISECONDS);
    }

    /**
     * @return Formatted string of the task being executed.
     */
    public String taskToString() {
        final StringJoiner fileInfo = new StringJoiner("; ");
        final StringJoiner sj = new StringJoiner(" ");
        Arrays.stream(this.command).forEach(x -> {
            if (x.length() > 10 && x.contains("\\")) {
                sj.add("\"...\"");
                fileInfo.add(x);
            } else {
                sj.add(x);
            }
        });
        return String.format("%s%n\t\t%s", sj.toString(), fileInfo.toString());
    }

    /**
     * Pings the IO handle with line data.
     *
     * @param is Process input stream.
     */
    @SneakyThrows
    private void ioHandle(final InputStream is) {
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
        );
        String x;
        while ((x = reader.readLine()) != null) {
            onLineFeed.accept(x);
        }
        reader.close();
    }

    /**
     * Forcefully stops the currently executing task.
     *
     * @return The exit code for the task.
     * @throws InterruptedException  Iff while waiting, interrupted.
     * @throws IllegalStateException Iff no task has started.
     */
    public int forceStop() throws InterruptedException {
        if (getLatestTask().isPresent()) {
            final Process p = getLatestTask().getValue();
            if (p.isAlive()) {
                return p.destroyForcibly().waitFor();
            } else {
                return p.exitValue();
            }
        } else {
            throw new IllegalStateException("Task not started!");
        }
    }
}
