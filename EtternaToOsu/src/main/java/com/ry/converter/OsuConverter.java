package com.ry.converter;

import com.ry.etterna.util.MSDChart;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Java interface created on 24/06/2022 for usage in project My-Stuff. Interface
 * exists to abstract away the boilerplate of the implementation of each module,
 * i.e., Create .osu; create audio; create bg, whilst simultaneously permitting
 * a valid level of control over what files are accepted.
 *
 * @author -Ry
 */
public interface OsuConverter {

    /**
     * Creates an Etterna Stream of all files which are convertable to an osu!
     * file.
     *
     * @param root The root directory to search for Etterna Files.
     * @return Stream of all possible and valid convertable etterna files.
     * @throws IOException Iff the root directory is cannot be traversed.
     */
    Stream<MSDChart> streamConvertableFiles(File root) throws IOException;

    /**
     * Gets and perhaps creates an output song directory to place the output
     * files into.
     *
     * @param chart The chart to get the Song directory for.
     * @param outputDir The root output directory.
     * @return A new, or old, existing directory.
     */
    File getSongDir(MSDChart chart, File outputDir);

    /**
     * Creates in the provided output directory, the converted .osu file.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param audioName The name of the audio file; or if none exists, null.
     * @param bgName The name of the background file; or if none exists, null.
     */
    void createOsuFile(MSDChart chart, File songDir, String audioName, String bgName);

    /**
     * Creates in the provided output directory, the audio file for the chart.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param audioFile The absolute path to the audio file to convert.
     * @return The name of the audio file; this will be passed onwards to {@link
     * #createOsuFile(MSDChart, File, String, String)}
     */
    String createAudioFile(MSDChart chart, File songDir, String audioFile);

    /**
     * Creates in the provided output directory, the background image for the
     * chart.
     *
     * @param chart The chart to convert.
     * @param songDir The song directory from {@link #getSongDir(MSDChart,
     * File)}.
     * @param backgroundFile The absolute path to the background file to
     * convert.
     * @return The name of the background file; this will be passed onwards to
     * {@link #createOsuFile(MSDChart, File, String, String)}.
     */
    String createBackgroundFile(MSDChart chart, File songDir, String backgroundFile);
}
