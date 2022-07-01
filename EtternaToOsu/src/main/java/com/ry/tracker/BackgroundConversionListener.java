package com.ry.tracker;

import com.ry.etterna.util.MSDChart;

import java.io.File;

/**
 * Java interface created on 27/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface BackgroundConversionListener {
    void onMalformed(MSDChart chart, int exit, String[] ffmpegCommand);
    void onComplete(MSDChart chart, String backgroundFile, File outputFile);
}
