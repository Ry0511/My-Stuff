package com.ry.osu.builderRedone;

import com.ry.osu.builderRedone.util.TimingInfo;
import com.ry.useful.StringUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@Builder
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

    ///////////////////////////////////////////////////////////////////////////
    // Actual class code below.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Map of all changes made to the template file.
     */
    @Singular("setElement")
    @Getter(AccessLevel.PRIVATE)
    private final Map<Element, String> changeMap;

    /**
     * Constructs the template file from the base map.
     * @param map The base element map.
     */
    private TemplateFile(final Map<Element, String> map) {
        for (final Element e : Element.values()) {
            map.putIfAbsent(e, StringUtils.EMPTY_STRING);
        }
        this.changeMap = map;
    }

    /**
     * Constructs a template file.
     *
     * @param info The note information and timing information.
     * @param finaliser The action performed on the builder before building.
     * @return Built, builder.
     */
    public static TemplateFile from(final TimingInfo info,
                                    final UnaryOperator<TemplateFileBuilder> finaliser) {
        return finaliser.apply(builder()
                .setHitObjects(info.getHitObjects())
                .setTimingPoints(info.getTimingPoints()))
                .build();
    }

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

    ///////////////////////////////////////////////////////////////////////////
    // Builder.
    ///////////////////////////////////////////////////////////////////////////

    public static class TemplateFileBuilder {

        // I wrote a script to create most of this lol

        public TemplateFileBuilder setAudioFileName(final String audioFileName) {
            setElement(Element.AUDIO_FILE_NAME, audioFileName);
            return this;
        }

        public TemplateFileBuilder setAudioFileName(final Path root,
                                                    final Path audioFile) {
            setElement(Element.AUDIO_FILE_NAME, root.relativize(audioFile).toString());
            return this;
        }

        public TemplateFileBuilder setTitle(final String title) {
            setElement(Element.TITLE, title);
            return this;
        }

        public TemplateFileBuilder setTitleUnicode(final String titleUnicode) {
            setElement(Element.TITLE_UNICODE, titleUnicode);
            return this;
        }

        public TemplateFileBuilder setTitleBoth(final String title) {
            setElement(Element.TITLE, StringUtils.toAscii(title));
            setElement(Element.TITLE_UNICODE, title);
            return this;
        }

        public TemplateFileBuilder setArtist(final String artist) {
            setElement(Element.ARTIST, artist);
            return this;
        }

        public TemplateFileBuilder setArtistUnicode(final String artistUnicode) {
            setElement(Element.ARTIST_UNICODE, artistUnicode);
            return this;
        }

        public TemplateFileBuilder setArtistBoth(final String artist) {
            setElement(Element.ARTIST, StringUtils.toAscii(artist));
            setElement(Element.ARTIST_UNICODE, artist);
            return this;
        }

        public TemplateFileBuilder setCreator(final String creator) {
            setElement(Element.CREATOR, creator);
            return this;
        }

        public TemplateFileBuilder setVersion(final String version) {
            setElement(Element.VERSION, version);
            return this;
        }

        public TemplateFileBuilder setSource(final String source) {
            setElement(Element.SOURCE, source);
            return this;
        }

        public TemplateFileBuilder setTags(final String tags) {
            setElement(Element.TAGS, tags);
            return this;
        }

        public TemplateFileBuilder setHpDrain(final String hpDrain) {
            setElement(Element.HP_DRAIN, hpDrain);
            return this;
        }

        public TemplateFileBuilder setHpDrain(final double drain) {
            return setHpDrain("" + drain);
        }

        public TemplateFileBuilder setOverallDifficulty(final String overallDifficulty) {
            setElement(Element.OVERALL_DIFFICULTY, overallDifficulty);
            return this;
        }

        public TemplateFileBuilder setOverallDifficulty(final double overallDifficulty) {
            return setOverallDifficulty("" + overallDifficulty);
        }

        public TemplateFileBuilder setBackgroundFile(final String backgroundFile) {
            setElement(Element.BACKGROUND_FILE, backgroundFile);
            return this;
        }

        public TemplateFileBuilder setBackgroundFile(final Path root,
                                                     final Path bgFile) {
            setElement(Element.BACKGROUND_FILE, root.relativize(bgFile).toString());
            return this;
        }

        public TemplateFileBuilder setTimingPoints(final String timingPoints) {
            setElement(Element.TIMING_POINTS, timingPoints);
            return this;
        }

        public TemplateFileBuilder setTimingPoints(final List<TimingPoint> xs) {
            final StringJoiner sj = new StringJoiner(System.lineSeparator());
            xs.forEach(x -> sj.add(x.compile()));
            setElement(Element.TIMING_POINTS, sj.toString());
            return this;
        }

        public TemplateFileBuilder setHitObjects(final String hitObjects) {
            setElement(Element.HIT_OBJECTS, hitObjects);
            return this;
        }

        public TemplateFileBuilder setHitObjects(final List<HitObject> xs) {
            final StringJoiner sj = new StringJoiner(System.lineSeparator());
            xs.forEach(x -> sj.add(x.compile()));
            setElement(Element.HIT_OBJECTS, sj.toString());
            return this;
        }

        public TemplateFileBuilder streamElements(final BiConsumer<TemplateFileBuilder, Stream<Element>> action) {
            action.accept(this, Arrays.stream(Element.values()));
            return this;
        }
    }
}
