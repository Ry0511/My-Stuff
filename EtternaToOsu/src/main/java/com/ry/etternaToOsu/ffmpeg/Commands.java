package com.ry.etternaToOsu.ffmpeg;

import com.ry.useful.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

/**
 * Java class created on 13/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class Commands {

    private enum CommandElem {
        FFMPEG,
        INPUT,
        DELAY,
        OUTPUT;
    }

    private static void setElem(@NonNull final CommandElem e,
                                @NonNull final String v,
                                @NonNull final String[] args) {
        int i = 0;
        for (final String s : args) {
            if (s.contains(e.name())) {
                args[i] = args[i].replaceAll(
                        e.name(),
                        Matcher.quoteReplacement(v)
                );
                return;
            }
            ++i;
        }
    }

    @ToString
    @EqualsAndHashCode
    @SuperBuilder(setterPrefix = "ioSet")
    public static abstract class IO {

        /**
         * The ffmpeg file reference such as "ffmpeg" for path based execution.
         */
        private final String ffmpeg;

        /**
         * The target infile for this IO.
         */
        private final String inFile;

        /**
         * The target output file for this IO.
         */
        private final String outFile;

        protected void loadIO(final String[] args) {
            setElem(CommandElem.FFMPEG, ffmpeg, args);
            setElem(CommandElem.INPUT, StringUtils.quote(inFile), args);
            setElem(CommandElem.OUTPUT, StringUtils.quote(outFile), args);
        }

        public void print() {
            final StringJoiner args = new StringJoiner(", ", "[", "]");
            Arrays.stream(compile()).forEach(args::add);
            System.out.println(args.toString());
        }

        public abstract String[] compile();
    }

    @Value
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "set")
    public static class NegativeAudioOffset extends IO {

        // Start 'k' seconds into a song
//        private static final String[] ARGS = {
//                "FFMPEG", "-y", "-i", "INPUT",
//                "-ss", "DELAY", "OUTPUT"
//        };

        // Offset/Negative-Delay + Compress
        private static final String[] ARGS = {
                "FFMPEG", "-y", "-i", "INPUT",
                "-ss", "DELAY", "-preset",
                "veryslow", "OUTPUT"
        };

        /**
         * The delay in seconds for this audio offset. I.e., 1.0 means start one
         * second into the file.
         */
        String delay;

        public String[] compile() {
            final String[] xs = ARGS.clone();
            loadIO(xs);
            setElem(CommandElem.DELAY, delay, xs);
            return xs;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "set")
    public static class PositiveAudioOffset extends IO {

        // ffmpeg -i "X" -af "adelay=DELAY" "O"
        private static final String[] ARGS = {
                "FFMPEG", "-y", "-i", "INPUT",
                "-af", "\"adelay=DELAY\"",
                "OUTPUT"
        };

        /**
         * Delay in milliseconds to add to the start of the provided file.
         */
        String delay;

        /**
         * The number of channels to apply the delay to.
         */
        int numChannels;

        public String[] compile() {
            final String[] xs = ARGS.clone();
            loadIO(xs);

            // Build delay for all channels
            final StringJoiner sj = new StringJoiner("|");
            IntStream.range(0, numChannels).forEach(x -> sj.add(delay));
            setElem(CommandElem.DELAY, sj.toString(), xs);

            return xs;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(setterPrefix = "set")
    public static class CompressImage extends IO {
        private static final String[] ARGS = {
                "FFMPEG", "-y", "-i", "INPUT", "OUTPUT"
        };

        @Override
        public String[] compile() {
            final String[] xs = ARGS.clone();
            loadIO(xs);
            return xs;
        }
    }

    /**
     * Creates and populates the builder 'V' with the provided IO Data returning
     * the resulting builder.
     *
     * @param ffmpeg The ffmpeg file reference.
     * @param inFile The infile reference.
     * @param outFile The outfile reference.
     * @param a The supplier of some IO Builder, or a derivative of an IO
     * Builder.
     * @param <V> The type of the IO Builder.
     * @return The IO Builder.
     */
    @SuppressWarnings("unchecked")
    public static <V extends IO.IOBuilder<?, ?>> V ioBuilder(
            @NonNull final String ffmpeg,
            @NonNull final String inFile,
            @NonNull final String outFile,
            @NonNull final Supplier<V> a) {
        return (V) a.get()
                .ioSetFfmpeg(ffmpeg)
                .ioSetInFile(inFile)
                .ioSetOutFile(outFile);
    }
}
