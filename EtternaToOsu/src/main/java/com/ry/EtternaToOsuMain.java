package com.ry;

import com.ry.cli.CLI;
import com.ry.cli.ConversionConfig;
import com.ry.converter.OsuConverterHandler;
import com.ry.converter.OsuConverterImpl;
import com.ry.ffmpeg.AsyncFFMPEG;
import com.ry.ffmpeg.FFMPEG;
import com.ry.tracker.LoggingListener;
import lombok.SneakyThrows;
import lombok.Synchronized;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Java class created on 30/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class EtternaToOsuMain {

    public static void main(final String[] args) {
        // If help exists anywhere; print help.
        if (Arrays.stream(args).anyMatch(x -> x.equalsIgnoreCase("--help"))) {
            System.out.println(ConversionConfig.getHelp());

            // Else build configuration and convert.
        } else {
            try {
                final ConversionConfig cfg = ConversionConfig.builder()
                        .loadFromArgs(args)
                        .build();

                initConversionFor(cfg);

                // Builder.build() := Null Pointer iff input || output missing
            } catch (final NullPointerException ex) {
                final CLI input = ConversionConfig.getCliFor("--o");
                final CLI output = ConversionConfig.getCliFor("--i");
                System.out.printf("Input file specified by '%s' and output "
                        + "specified by '%s' is required. Run with '--help' "
                        + "for more info.%n",
                        Arrays.toString(input.value()),
                        Arrays.toString(output.value())
                );
                System.exit(-10);

                // Data processing exception
            } catch (final RuntimeException ex) {
                System.out.printf(
                        "One or more arguments are invalid; More info: '%s'%n",
                        ex.getMessage()
                );
            }
        }
    }

    private static void initConversionFor(final ConversionConfig cfg) {
        final File input = cfg.getInput();
        final File output = cfg.getOutput();
        final FFMPEG mpeg = cfg.getFfmpeg();

        final AsyncFFMPEG baseService = new AsyncFFMPEG(mpeg, cfg.getBaseRateService());
        final AsyncFFMPEG rateService = new AsyncFFMPEG(mpeg, cfg.getRatedService());

        final OsuConverterImpl impl = new OsuConverterImpl(
                cfg.getChartMappingFunction(),
                cfg.getChartFinaliser(),
                baseService,
                rateService
        );

        final Runnable waitAction;
        if (cfg.isLogEnabled()) {
            final LoggingListener ls = new LoggingListener();
            ls.setMode(cfg.getLogMode());
            impl.getTracker().addBackgroundListener(ls);
            impl.getTracker().addAudioConversionListener(ls);
            waitAction = ls::printState;
        } else {
            waitAction = () -> {};
        }

        final OsuConverterHandler<OsuConverterImpl> handle = new OsuConverterHandler<>(impl);
        try {
            System.out.println(cfg.getConfigAsString());
            handle.convert(input, output);

            // Wait for termination
            baseService.terminateAndWait(waitAction);
            rateService.terminateAndWait(waitAction);
        } catch (final Exception e) {
            throw new Error(e);
        }

        System.out.println("[INFO] Exiting...");
    }
}