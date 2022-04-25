package com.ry.osu.builder;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.ry.osu.builder.Template.TemplateElement.ARTIST;
import static com.ry.osu.builder.Template.TemplateElement.BG_FILE;
import static com.ry.osu.builder.Template.TemplateElement.CREATOR;
import static com.ry.osu.builder.Template.TemplateElement.HIT_OBJECTS;
import static com.ry.osu.builder.Template.TemplateElement.SOURCE;
import static com.ry.osu.builder.Template.TemplateElement.TAGS;
import static com.ry.osu.builder.Template.TemplateElement.TIMING_POINTS;
import static com.ry.osu.builder.Template.TemplateElement.TITLE;
import static com.ry.osu.builder.Template.TemplateElement.VERSION;

/**
 * Java class created on 24/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Value
@Builder(setterPrefix = "set")
public class BuildableOsuFile {

    ///////////////////////////////////////////////////////////////////////////
    // This isn't a functional osu file it's just a placeholder for writing
    // values to a file system.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Title of this chart defaults to Unknown.
     */
    @Builder.Default
    String title = "Unknown Title";

    /**
     * artist of this chart defaults to Unknown.
     */
    @Builder.Default
    String artist = "Unknown Artist";

    /**
     * creator of this chart defaults to Unknown.
     */
    @Builder.Default
    String creator = "Unknown Creator";

    /**
     * version of this chart defaults to Unknown.
     */
    @Builder.Default
    String version = "Unknown Version";

    /**
     * source of this chart defaults to Unknown.
     */
    @Builder.Default
    String source = "Unknown Source";

    /**
     * tags of this chart defaults to Empty.
     */
    @Builder.Default
    String tags = "";

    /**
     * background file of this chart defaults to Empty.
     */
    @Builder.Default
    String backgroundFile = "";

    /**
     * Timing points for this chart, defaults to the empty list.
     */
    @Builder.Default
    List<? extends TimingPoint> timingPoints = new ArrayList<>();

    /**
     * Hit objects for this chart, defaults to the empty list.
     */
    @Builder.Default
    List<? extends HitObject> hitObjects = new ArrayList<>();

    /**
     * @return Full file content
     */
    public String asOsuStr() {
        String s = Template.getTemplateContent();

        // Meta
        s = TITLE.replaceMe(s, getTitle());
        s = ARTIST.replaceMe(s, getArtist());
        s = CREATOR.replaceMe(s, getCreator());
        s = VERSION.replaceMe(s, getVersion());
        s = SOURCE.replaceMe(s, getSource());
        s = TAGS.replaceMe(s, getTags());
        s = BG_FILE.replaceMe(s, getBackgroundFile());

        // Timing points
        final StringJoiner tpSj = new StringJoiner(System.lineSeparator());
        timingPoints.forEach(x -> tpSj.add(x.asOsuStr()));
        s = TIMING_POINTS.replaceMe(s, tpSj.toString());

        final StringJoiner hoSj = new StringJoiner(System.lineSeparator());
        hitObjects.forEach(x -> hoSj.add(x.asOsuStr()));
        s = HIT_OBJECTS.replaceMe(s, hoSj.toString());

        return s;
    }
}
