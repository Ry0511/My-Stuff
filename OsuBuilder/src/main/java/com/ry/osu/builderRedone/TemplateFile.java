package com.ry.osu.builderRedone;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class TemplateFile {

    /**
     * The template file resource.
     */
    private static final URL TEMPLATE_FILE = TemplateFile.class.getResource("Template.txt");

    /**
     * The total number of characters in the template file.
     */
    private static final int CONTENT_LENGTH = 1180;

    /**
     * The whole template file loaded into a single string. This avoid
     */
    private static final String TEMPLATE_CONTENT;

    static {
        if (TEMPLATE_FILE == null) {
            throw new Error("Template file failed to load.");
        } else {
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(TEMPLATE_FILE.openStream()))) {
                final String lineSep = System.lineSeparator();
                final StringBuffer sb = new StringBuffer(CONTENT_LENGTH);
                in.lines().forEach(x -> sb.append(x).append(lineSep));
                TEMPLATE_CONTENT = sb.toString();

                // Re-throw as this issue is terminal
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Map of all changes made to the template file.
     */
    @Getter(AccessLevel.PRIVATE)
    private final Map<Element, String> changeMap = new HashMap<>();

    /**
     * Gets the mapped element value.
     *
     * @param elem The element to get.
     * @return The mapped value, or null if no mapping is found.
     */
    public String getElement(final Element elem) {
        return changeMap.get(elem);
    }

    /**
     * Sets the provided elements value.
     *
     * @param elem The element to set.
     * @param value The value to set.
     */
    public void setElement(final Element elem, final String value) {
        this.changeMap.put(elem, value);
    }

    /**
     * Compiles this current template to a complete file ready to store.
     *
     * @return Complete .osu string content.
     */
    public String compile() {
        String content = TEMPLATE_CONTENT;

        for (final var x : changeMap.entrySet()) {
            content = content.replaceFirst(
                    x.getKey().getName(),
                    Matcher.quoteReplacement(x.getValue())
            );
        }

        return content;
    }

    /**
     * Enumerations for all the elements that can be edited from the template
     * file.
     */
    public static enum Element {
        AUDIO_FILE_NAME("___AUDIO_FILE_NAME"),
        TITLE("___TITLE"),
        TITLE_UNICODE("___TITLE_UNICODE"),
        ARTIST("___ARTIST"),
        ARTIST_UNICODE("___ARTIST_UNICODE"),
        CREATOR("___CREATOR"),
        VERSION("___VERSION"),
        SOURCE("___SOURCE"),
        TAGS("___TAGS"),
        HP_DRAIN("___HP_DRAIN"),
        OVERALL_DIFFICULTY("___OVERALL_DIFFICULTY"),
        BACKGROUND_FILE("___BACKGROUND_FILE"),
        TIMING_POINTS("___TIMING_POINTS"),
        HIT_OBJECTS("___HIT_OBJECTS");

        @Getter
        private final String name;

        Element(final String s) {
            name = Pattern.quote(s);
        }
    }
}
