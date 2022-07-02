package com.ry.tracker;

import com.ry.etterna.util.MSDChart;

/**
 * Java interface created on 27/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface AudioConversionListener {
    void onSubmitted();
    void onMalformed(MSDChart chart, String[] ffmpegCommand, int exit);
    void onComplete(MSDChart chart, String[] ffmpegCommand);
}
