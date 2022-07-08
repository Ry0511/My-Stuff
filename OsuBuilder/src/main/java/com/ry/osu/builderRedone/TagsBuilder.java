package com.ry.osu.builderRedone;

import java.util.StringJoiner;

/**
 * Java class created on 28/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class TagsBuilder {

    private final StringJoiner tags = new StringJoiner(",");

    public TagsBuilder add(final String tag) {
        tags.add(tag);
        return this;
    }

    public TagsBuilder add(final String tag, Object... args) {
        tags.add(String.format(tag, args));
        return this;
    }

    public String build() {
        return tags.toString();
    }
}
