package com.ry.ffmpeg;

import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Java class created on 19/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@NoArgsConstructor
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

    /**
     * Gets a ffmpeg instance from the users PATH.
     *
     * @return Empty optional if ffmpeg could not be found on their PATH. Else
     * ffmpeg instance if it was detected.
     * @implNote It is possible that a non-empty optional can be returned and
     * ffmpeg is not present. This means that the user has some derivative of
     * ffmpeg/bin but said folder doesn't contain an executable named
     * ffmpeg.exe.
     */
    public static Optional<FFMPEG> getFfmpegFromPath() {
        final Pattern rgx = Pattern.compile("(?i)(ffmpeg.bin)|(ffmpeg\\.exe)");

        if (rgx.matcher(System.getenv("Path")).find()) {
            return Optional.of(new FFMPEG("ffmpeg", false));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets a ffmpeg instance by finding any matching ffmpeg.exe from the root
     * folder provided.
     *
     * @param root The folder check, any depth is checked.
     * @return Empty optional in any malformed case or if ffmpeg.exe was not
     * found. If ffmpeg.exe was found (case-insensitive) then a new FFMPEG
     * instance with said file is returned.
     */
    public static Optional<FFMPEG> getFfmpegFromRoot(final File root) {
        if (root.isFile() || !root.exists()) {
            return Optional.empty();
        }

        try {
            return Files.walk(root.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(x -> x.getName().equalsIgnoreCase("ffmpeg.exe"))
                    .findAny()
                    .map(x -> new FFMPEG(x.getAbsolutePath(), true));

        } catch (final IOException ignored) {
            // Ignore
        }

        return Optional.empty();
    }

    /**
     * @return Empty if ffmpeg could not be found in the working directory. Else
     * a new instance.
     * @see #getFfmpegFromRoot(File)
     */
    public static Optional<FFMPEG> getFfmpegFromWorkingDir() {
        return getFfmpegFromRoot(new File(System.getProperty("user.dir")));
    }

    /**
     * Creates a delayed audio command.
     *
     * @param delay The delay to apply is Seconds.
     * @param input The input file to delay.
     * @param output The output file to produce.
     * @return Array of FFMPEG CLI arguments.
     */
    public static IOCommand delayAudio(final BigDecimal delay,
                                       final String input,
                                       final String output) {

        // Positive Offset (Applies to 4 audio channels excess doesn't matter)
        if (delay.signum() > -1) {
            final String s = delay.multiply(MILLIS_FACTOR, C)
                    .toBigInteger()
                    .toString();
            final String delayStr = String.format("%s|%s|%s|%s", s, s, s, s);

            // Convert audio
            return IOCommand.builder()
                    .isQuotePaths(true)
                    .addGlobalFlag("-y")
                    .inputFile(input)
                    .outputOptions(List.of(
                            "-af",
                            "\"adelay=" + delayStr + "\"",
                            "-map",
                            "0:a"))
                    .outputFile(output)
                    .build();

            // Negative offset
        } else {
            return IOCommand.builder()
                    .isQuotePaths(true)
                    .addGlobalFlag("-y")
                    .inputFile(input)
                    .outputOptions(List.of(
                            "-ss",
                            delay.negate().toPlainString(),
                            "-map",
                            "0:a"))
                    .outputFile(output)
                    .build();
        }
    }

    /**
     * Rates the input file to the provided rate.
     *
     * @param input The input file to rate
     * @param rate The desired audio rate Min: 0.5, Max: 2.0
     * @param isNightcore Use Night core for the output rate.
     * @param output The output file destination.
     * @return Future representing this tasks state.
     * @implNote The NC mode, although works should still be tweaked.
     */
    public static IOCommand rateAudio(final String input,
                                      final String rate,
                                      final boolean isNightcore,
                                      final String output) {
        if (!isNightcore) {
            return IOCommand.builder()
                    .isQuotePaths(true)
                    .addGlobalFlag("-y")
                    .inputFile(input)
                    .outputOptions(List.of("-filter:a", "\"atempo=" + rate + "\"", "-vn"))
                    .outputFile(output)
                    .build();

            // Primitive Nightcore (Untested as of 19/06/2022)
        } else {
            // todo test this
            System.err.println("[WARNING] Producing rate command using nightcore this is experimental and may not work...");
            return IOCommand.builder()
                    .isQuotePaths(true)
                    .addGlobalFlag("-y")
                    .inputFile(input)
                    .outputOptions(List.of("-filter:a", "\"asetrate=44100*" + rate + "\"", "-vn"))
                    .outputFile(output)
                    .build();
        }
    }

    /**
     * Creates an image compression command.
     *
     * @param img The image to compress.
     * @param output The output image, note that '.jgp' is appended to the
     * name.
     * @return Built IOCommand.
     */
    public static IOCommand compressImage(final String img,
                                          final String output) {
        return IOCommand.builder()
                .isQuotePaths(true)
                .addGlobalFlag("-y")
                .inputFile(img)
                .outputFile(output + ".jpg")
                .build();
    }
}
