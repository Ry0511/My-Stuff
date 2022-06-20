package com.ry.ffmpeg;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.stream.Stream;

/**
 * Java class created on 19/06/2022 for usage in project My-Stuff. IOCommand
 * wraps a basic Input & Output ffmpeg command.
 *
 * @author -Ry
 */
@Data
@Builder
public class IOCommand {

    /**
     * If true when building the command the Input file and Output Files will be
     * encased in double quotes.
     */
    private final boolean isQuotePaths;

    /**
     * The global flags to apply to the command.
     */
    @Singular("addGlobalFlag")
    private final List<String> globalFlags;

    /**
     * The input file path, this string is taken literally as is.
     */
    private final String inputFile;

    /**
     * The options to apply to the input file.
     */
    @Singular("addInputOption")
    private final List<String> inputOptions;

    /**
     * The output options to apply.
     */
    @Singular("addOutputOption")
    private final List<String> outputOptions;

    /**
     * The output file, the string is taken literally as is.
     */
    private final String outputFile;

    /**
     * Event handle for when this command is executed.
     */
    private ExecutionListener listener;

    /**
     * @return The number of arguments in this command.
     */
    public int countArgs() {
        return 2 + globalFlags.size() + inputOptions.size() + outputOptions.size();
    }

    /**
     * @return Get the input file path.
     */
    public String getInputFile() {
        if (isQuotePaths) {
            return "\"" + inputFile + "\"";
        } else {
            return inputFile;
        }
    }

    /**
     * @return Get the output file path.
     */
    public String getOutputFile() {
        if (isQuotePaths) {
            return "\"" + outputFile + "\"";
        } else {
            return outputFile;
        }
    }

    /**
     * Builds this command using the provided ffmpeg executable. The output
     * format looks like this:
     * <pre><code>
     *     ffmpeg [options]
     *     [[infile options] -i infile]...
     *     {[outfile options] outfile}...
     * </pre></code>
     *
     * @param ffmpeg The ffmpeg reference.
     * @return Full command args.
     */
    public String[] build(final String ffmpeg) {
        final Stream.Builder<String> builder = Stream.builder();
        builder.add(ffmpeg);
        globalFlags.forEach(builder::add);
        inputOptions.forEach(builder::add);
        builder.add("-i").add(getInputFile());
        outputOptions.forEach(builder::add);
        builder.add(getOutputFile());

        return builder.build().toList().toArray(String[]::new);
    }

    /**
     * Command Execution listener which allows the three phases of execution to
     * be tracked. That being, started, running, and completed.
     */
    public interface ExecutionListener {

        /**
         * Invoked when this command has started execution.
         *
         * @param cmd The command that has started execution.
         */
        void onStart(final IOCommand cmd);

        /**
         * Invoked whenever the standard output has received a new line.
         *
         * @param cmd The command that was executed.
         * @param line The raw line.
         */
        void onMessage(final IOCommand cmd, final String line);

        /**
         * Invoked when the command has been completed.
         *
         * @param cmd The command that has finished.
         * @param exit The exit code for the process.
         */
        void onComplete(final IOCommand cmd, final int exit);
    }
}
