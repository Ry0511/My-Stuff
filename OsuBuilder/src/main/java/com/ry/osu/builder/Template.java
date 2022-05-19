package com.ry.osu.builder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class created on 24/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Template {

    private static final File TEMPLATE = new File(
            "OsuBuilder/src/main/java/com/ry/osu/builder/Template.txt"
    );

    /**
     * Since strings are immutable we can store the full content in memory
     * and the just clone this one whenever a change is required.
     */
    private static final String CONTENT;

    static {
        try {
            CONTENT = FileUtils.readFileToString(
                    TEMPLATE,
                    StandardCharsets.UTF_8
            );

            // Shouldn't happen
        } catch (final IOException e) {
            throw new Error(String.format(
                    "File Read Error '%s' '%s'...",
                    TEMPLATE,
                    e.getMessage()
            ));
        }
    }

    /**
     * @return Template file content.
     */
    public static String getTemplateContent() {
        return CONTENT;
    }

    public enum TemplateElement {
        AUDIO_FILE,
        TITLE,
        ARTIST,
        CREATOR,
        VERSION,
        SOURCE,
        TAGS,
        BG_FILE,
        TIMING_POINTS,
        HIT_OBJECTS,
        BEATMAP_SET_ID,
        BEATMAP_ID;

        private static final String[] REPLACE_TARGET = {
                "___AUDIO-FILE-NAME",
                "___TITLE",
                "___ARTIST",
                "___CREATOR",
                "___VERSION",
                "___SOURCE",
                "___TAGS",
                "___BACKGROUND-FILE",
                "___TIMING-POINTS",
                "___HIT-OBJECTS"
        };

        /**
         * Replaces this element with the provided element.
         *
         * @param content The content string to replace from.
         * @param replacement The replacement string.
         * @return The content to replace with.
         */
        public String replaceMe(final String content,
                                final String replacement) {
            return content.replaceAll(
                    Pattern.quote(REPLACE_TARGET[ordinal()]),
                    Matcher.quoteReplacement(replacement)
            );
        }
    }
}
