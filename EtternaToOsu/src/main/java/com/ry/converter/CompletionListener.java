package com.ry.converter;

import com.ry.ffmpeg.IOCommand;

import java.util.Arrays;

/**
 * Java interface created on 25/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface CompletionListener extends IOCommand.ExecutionListener {

    /**
     * Invoked when this command has started execution.
     *
     * @param cmd The command that has started execution.
     * @param p The underlying process that was started.
     */
    @Override
    default void onStart(final IOCommand cmd, final Process p) {
        // Do nothing
    }

    /**
     * Invoked whenever the standard output has received a new line.
     *
     * @param cmd The command that was executed.
     * @param line The raw line.
     */
    @Override
    default void onMessage(final IOCommand cmd, final String line) {
        // Do nothing
    }
}
