package com.ry.ffmpeg;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * Java class created on 23/04/2022 for usage in project FunctionalUtils.
 * Utility class which will use the default {@link FFMPEG#INSTANCE} to execute
 * the subject commands.
 *
 * @author -Ry
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FFMPEGUtils {

    /**
     * Default math context used for scaling.
     */
    private static final MathContext C
            = new MathContext(12, RoundingMode.HALF_UP);

    /**
     * Milliseconds scale factor.
     */
    private static final BigDecimal MILLIS_FACTOR = new BigDecimal("1000");

    ///////////////////////////////////////////////////////////////////////////
    // Command implementations
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a delayed audio command.
     *
     * @param delay The delay to apply.
     * @param input The input file to delay.
     * @param output The output file to produce.
     * @return Array of FFMPEG CLI arguments.
     */
    public static String[] delayAudio(final BigDecimal delay,
                                      final String input,
                                      final String output) {

        // Positive Offset (Applies to 4 audio channels excess doesn't matter)
        if (delay.signum() > -1) {
            final String s = delay.multiply(MILLIS_FACTOR, C)
                    .toBigInteger()
                    .toString();
            final String delayStr = String.format("%s|%s|%s|%s", s, s, s, s);

            // Convert audio
            return CommandBuilder.builder()
                    .add("-y", "-i", quote(input))
                    .add("-af", quote("adelay=" + delayStr))
                    .add("-map", "0:a")
                    .add(quote(output))
                    .build();

            // Negative offset
        } else {
            return CommandBuilder.builder()
                    .add("-y", "-i", quote(input))
                    .add("-ss", delay.negate().toPlainString())
                    .add("-map", "0:a")
                    .add(quote(output))
                    .build();
        }
    }

    /**
     * Copies the input file to a temporary file and then compresses the clone
     * over the original, that is, the input file is modified only in the
     * underlying data.
     *
     * @param input The input file to compress.
     * @return Future of this tasks state.
     */
    public static Future<Process> compressAudio(
            @NonNull final File input) throws IOException {
        final String name = input.getName();
        final String ext = name.substring(name.lastIndexOf("."));

        final Path tmp = Files.createTempFile(
                "Compress-Audio-Task",
                ext
        );
        FileUtils.copyFile(input, tmp.toFile());

        return FFMPEG.INSTANCE.exec(CommandBuilder.builder().add(
                        "-y", "-i", quote(tmp.toFile().getAbsolutePath()),
                        "-preset", "veryslow", quote(input.getAbsolutePath())
                ).build()
        );
    }

    /**
     * Async image compression, that is, the resulting input image is converted
     * to '.jpg' using FFMPEG's default compression.
     *
     * @param input The input file to compress.
     * @param output The output file to produce.
     * @return Future representing the tasks current state.
     */
    public static Future<Process> compressImage(final String input,
                                                final String output) {
        return FFMPEG.INSTANCE.exec(CommandBuilder.builder().add(
                        "-y", "-i", quote(input), quote(output + ".jpg")
                ).build()
        );
    }

    /**
     * Rates the input file to the provided rate.
     *
     * @param input The input file to rate
     * @param rate The desired audio rate Min: 0.5, Max: 2.0
     * @param isNC Use Night core for the output rate.
     * @param output The output file destination.
     * @return Future representing this tasks state.
     * @implNote The NC mode, although works should still be tweaked.
     */
    public static String[] rateAudio(final String input,
                                     final String rate,
                                     final boolean isNC,
                                     final String output) {
        if (!isNC) {
            return CommandBuilder.builder().add(
                    "-y", "-i", quote(input),
                    "-filter:a",
                    quote("atempo=" + rate),
                    "-vn",
                    quote(output)
            ).build();

            // Primitive Nightcore
        } else {
            return CommandBuilder.builder().add(
                    "-y", "-i", quote(input),
                    "-filter:a",
                    String.format("asetrate=44100*%s", rate),
                    "-vn",
                    quote(output)
            ).build();
        }
    }

    /**
     * Quotes the provided input string.
     *
     * @param a The string to quote.
     * @return "a"
     */
    public static String quote(@NonNull final String a) {
        return "\"" + a + "\"";
    }
}
