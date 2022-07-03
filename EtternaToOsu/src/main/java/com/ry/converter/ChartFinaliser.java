package com.ry.converter;

import com.ry.etterna.util.MSDChart;
import com.ry.osu.builderRedone.TemplateFile;

/**
 * Java interface created on 27/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface ChartFinaliser {

    TemplateFile mapToTemplate(MSDChart chart, String audioName, String bgName);
    String getChartFilename(MSDChart chart);
}
