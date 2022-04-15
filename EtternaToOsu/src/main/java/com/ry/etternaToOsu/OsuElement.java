package com.ry.etternaToOsu;

import com.ry.useful.StringUtils;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java enum created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@ToString
public enum OsuElement {
    AUDIO_FILE_NAME,
    TITLE,
    TITLE_UNICODE,
    ARTIST,
    ARTIST_UNICODE,
    CREATOR,
    VERSION,
    SOURCE,
    TAGS;

    private final Pattern regex;

    OsuElement() {
        this.regex = Pattern.compile(String.format(
                "(?i)%s:.*",
                name().replaceAll("_", "")
        ));
    }

    /**
     * Sets in the provided content string this element.
     *
     * @param content The content to up
     * @param replacement The content to set.
     * @return Content string which may have been changed, depending on whether
     * the Element could be found or not.
     */
    public String setThis(final String content, final String replacement) {
        final Optional<String> me = StringUtils.get(
                regex.matcher(content),
                Matcher::find,
                Matcher::group
        );

        if (me.isPresent()) {
            final int x = me.get().indexOf(":");
            return content.replaceFirst(
                    Pattern.quote(me.get()),
                    Matcher.quoteReplacement(me.get().replaceFirst(
                            me.get().substring(x), ":" + replacement))
            );
        } else {
            return content;
        }
    }
}
