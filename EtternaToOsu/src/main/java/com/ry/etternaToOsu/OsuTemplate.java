package com.ry.etternaToOsu;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class OsuTemplate {

    /**
     * The template file to modify.
     */
    private static final File TEMPLATE_FILE = new File(
            "G:\\Dev\\Projects\\IntelliJ\\FunctionalUtils\\EtternaToOsu"
                    + "\\src\\main\\resources\\OsuTemplate.txt");

    /**
     * A little more of a complex element to set so just replace this, then
     * you're fucked if you wanna make another change.
     */
    private static final String BG_FILE_REPLACE
            = "___BACKGROUND-FILE-REPLACE-ME";

    /**
     * Timing points.
     */
    private static final String TIMING_POINTS_REPLACE
            = "___TIMING-POINTS-REPLACE-ME";

    /**
     * Hit objects.
     */
    private static final String HIT_OBJECTS_REPLACE
            = "___HIT-OBJECTS-REPLACE-ME";

    @Setter(AccessLevel.PRIVATE)
    private String content;

    public OsuTemplate() throws IOException {
        content = FileUtils.readFileToString(
                TEMPLATE_FILE,
                StandardCharsets.UTF_8
        );
    }

    /**
     * Sets the provided element in full to the provided content.
     *
     * @param element The element to set.
     * @param content The content to write.
     */
    public void setElement(final OsuElement element,
                           final String content) {
        this.content = element.setThis(getContent(), content);
    }

    /**
     * Sets the background filename to the provided name.
     *
     * @param name The name of the background file.
     * @implNote The content string is quoted before being written.
     */
    public void setBackgroundFile(final String name) {
        this.content = content.replaceFirst(
                Pattern.quote(BG_FILE_REPLACE),
                Matcher.quoteReplacement(name)
        );
    }

    /**
     * Sets the timing points to the provided timing point data.
     *
     * @param timingPoints The timing points to set.
     * @implNote The content string is quoted before being written.
     */
    public void setTimingPoints(final String timingPoints) {
        this.content = content.replaceFirst(
                Pattern.quote(TIMING_POINTS_REPLACE),
                Matcher.quoteReplacement(timingPoints)
        );
    }

    /**
     * Sets the hit object data to the provided hit objects.
     *
     * @param hitObjects The hit objects to set.
     * @implNote The content string is quoted before being written.
     */
    public void setHitObjects(final String hitObjects) {
        this.content = content.replaceFirst(
                Pattern.quote(HIT_OBJECTS_REPLACE),
                Matcher.quoteReplacement(hitObjects)
        );
    }

    /**
     * Attempts to create the provided osu file and populate with data.
     *
     * @param f The file to create and write to.
     * @throws IOException Iff the file doesn't exist after a creation attempt
     *                     has been made, or if the write fails.
     */
    public void createAndWrite(final File f) throws IOException {
        if (f.isFile() || f.createNewFile()) {
            FileUtils.write(f, content, StandardCharsets.UTF_8);
        }
    }
}
