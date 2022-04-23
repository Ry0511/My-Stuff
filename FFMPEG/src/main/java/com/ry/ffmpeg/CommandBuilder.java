package com.ry.ffmpeg;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Java class created on 23/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data(staticConstructor = "builder")
public class CommandBuilder implements Cloneable {

    private final ArrayList<String> args = new ArrayList<>();

    public CommandBuilder add(final String arg) {
        this.args.add(arg);
        return this;
    }

    public CommandBuilder add(final String... args) {
        Arrays.stream(args).forEach(this::add);
        return this;
    }

    public CommandBuilder mutate(final String target,
                                 final String newValue) {
        for (int i = 0; i < args.size(); ++i) {
            if (target.equals(args.get(i))) {
                args.set(i, newValue);
            }
        }

        return this;
    }

    public CommandBuilder mutate(final Predicate<String> target,
                                 final Function<String, String> action) {
        for (int i = 0; i < args.size(); ++i) {
            final String x = args.get(i);
            if (target.test(x)) {
                this.args.set(i, action.apply(x));
            }
        }

        return this;
    }

    public CommandBuilder replaceFirst(final String regex,
                                       final String replacement) {
        for (int i = 0; i < args.size(); ++i) {
            final String x = args.get(i);
            if (x.matches(regex)) {
                this.args.set(i, replacement);
                return this;
            }
        }

        return this;
    }

    /**
     * @return This command as a string array of arguments.
     */
    public String[] build() {
        return args.toArray(String[]::new);
    }

    /**
     * @return Clone of this.
     */
    @Override
    public CommandBuilder clone() {
        try {
            return (CommandBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
