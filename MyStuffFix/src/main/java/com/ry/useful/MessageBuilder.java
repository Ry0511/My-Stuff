package com.ry.useful;

import lombok.Data;

/**
 * Java class created on 11/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class MessageBuilder {

    private final StringBuilder msg;
    private boolean isAutoClamp;
    private boolean isAutoInsertNewLine;
    private final int spacing;

    public static MessageBuilder builder(final int spacing) {
        return new MessageBuilder(spacing);
    }

    private MessageBuilder(final int spacing) {
        this.msg = new StringBuilder();
        this.spacing = spacing;
    }

    public MessageBuilder setAutoNewline(final boolean state) {
        this.isAutoInsertNewLine = state;
        return this;
    }

    public MessageBuilder setAutoClamp(final boolean state) {
        this.isAutoClamp = state;
        return this;
    }

    public MessageBuilder addLineSeparator() {
        this.msg.append(System.lineSeparator());
        return this;
    }

    public MessageBuilder add(final String... strings) {

        for (final String str : strings) {
            final int requiredSpacing = spacing - str.length();

            if (requiredSpacing < 0) {
                if (!isAutoClamp) {
                    throw new IllegalStateException(String.format(
                            "Cannot insert String '%s' to builder as it "
                                    + "violates size restrictions.", str
                    ));
                } else {
                    addString(str.substring(0, spacing), 0);
                }

            } else {
                addString(str, requiredSpacing);
            }
        }
        if (isAutoInsertNewLine) addLineSeparator();
        return this;
    }

    private void addString(final String str, final int requiredSpacing) {
        msg.append(str);
        msg.append(" ".repeat(Math.max(0, requiredSpacing)));
    }

    public String build() {
        return msg.toString();
    }
}
