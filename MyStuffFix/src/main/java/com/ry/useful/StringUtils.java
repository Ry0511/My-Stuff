package com.ry.useful;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.lang.reflect.MalformedParametersException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    /**
     * Collects the first match found for the provided matcher.
     *
     * @param m The matcher to use.
     * @param predicate The predicate to test.
     * @param resultExtractor The result extractor.
     * @return Empty optional iff no match was found, else Optional of the
     * result from result extractor.
     */
    public static Optional<String> get(
            @NonNull final Matcher m,
            @NonNull final Predicate<Matcher> predicate,
            @NonNull final Function<Matcher, String> resultExtractor) {

        if (predicate.test(m)) {
            return Optional.of(resultExtractor.apply(m));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Collects all matches found using the given matcher, predicate, and
     * collecting function.
     *
     * @param m The matcher to process.
     * @param predicate The search/seek function.
     * @param resultExtractor The Matcher -> String function.
     * @return All matches, even if none.
     */
    public static List<String> getAll(
            @NonNull final Matcher m,
            @NonNull final Predicate<Matcher> predicate,
            @NonNull final Function<Matcher, String> resultExtractor) {

        final List<String> results = new ArrayList<>();

        while (predicate.test(m)) {
            results.add(resultExtractor.apply(m));
        }

        return results;
    }

    /**
     * @param m Matcher attached to a subject string.
     * @param extractor The extraction function.
     * @param action The action to apply.
     */
    public static void forAllMatches(
            @NonNull final Matcher m,
            @NonNull final Function<Matcher, String> extractor,
            @NonNull final Consumer<String> action) {
        while (m.find()) {
            action.accept(extractor.apply(m));
        }
    }

    /**
     * @param regex The regex to compile.
     * @param subject The string to read.
     * @return Matcher with the provided regex attached to the subject string.
     * @see Pattern#compile(String)
     * @see Pattern#matcher(CharSequence)
     */
    public static Matcher matcherOf(@NonNull final String regex,
                                    @NonNull final String subject) {
        return Pattern.compile(regex).matcher(subject);
    }

    /**
     * @param s The string to test.
     * @return {@code true} if s is a valid integer.
     */
    public static boolean isInteger(@NonNull final String s) {
        return s.matches("-?[0-9]+");
    }

    /**
     * Parses the string value as if it were an integer value.
     *
     * @param s The string to parse.
     * @return Int Min/Max if OOB, else iff 's' is an int then s.
     * @throws NumberFormatException If s is not a valid number.
     */
    public static int parseInt(final String s) {
        final BigInteger v = new BigInteger(s);

        // Max int
        if (v.compareTo(Constants.MAX_INT) > 0) {
            return Integer.MAX_VALUE;
            // Min int
        } else if (v.compareTo(Constants.MIN_INT) < 0) {
            return Integer.MIN_VALUE;
            // int
        } else {
            return v.intValue();
        }
    }

    /**
     * @param s The string to test.
     * @return {@code true} if s is an FPU.
     */
    public static boolean isFloat(@NonNull final String s) {
        return Constants.DECIMAL_REGEX.matcher(s).find();
    }

    /**
     * Parses the literal string is if it were a Double value.
     *
     * @param s The string to parse.
     * @return Maximum or Minimum value of double if OOB, else the Double value.
     * @throws NumberFormatException If s is not an FPU.
     */
    public static double parseDouble(@NonNull final String s) {
        final BigDecimal v = new BigDecimal(s);

        // Max float
        if (v.compareTo(Constants.MAX_DOUBLE) > 0) {
            return Integer.MAX_VALUE;
            // Min float
        } else if (v.compareTo(Constants.MIN_DOUBLE) < 0) {
            return Integer.MIN_VALUE;
            // float
        } else {
            return v.intValue();
        }
    }

    /**
     * Quotes the provided string with the provided quote string.
     *
     * @param a The string to quote.
     * @param quote The quote string to use.
     * @return quote + a + quote
     */
    public static String quote(@NonNull final String a,
                               @NonNull final String quote) {
        return quote + a + quote;
    }

    /**
     * Quotes the provided string with "s"
     *
     * @param s The string to quote.
     * @return "s"
     */
    public static String quote(@NonNull final String s) {
        return quote(s, "\"");
    }

    /**
     * @param s The filename.
     * @param withDot Contains the '.' in the returned result.
     * @return The extension of the provided string, that is, the last instance
     * of . to the end of the string.
     */
    public static String getFileExtension(final String s,
                                          final boolean withDot) {

        final int last = s.lastIndexOf(".");

        if (last == -1) {
            throw new Error("Not a file: " + s);
        }

        // iff last + 1 > length := OOBEx
        return withDot ? s.substring(last) : s.substring(last + 1);
    }

    /**
     * @param s The full file to get the filename of.
     * @return The filename of the specified full file path.
     */
    public static String getFileName(@NonNull final String s) {

        final int last = s.lastIndexOf(".");

        if (last == -1) {
            throw new MalformedParametersException("Not a File: " + s);
        }

        return s.substring(0, last);
    }

    /**
     * Convenience method for {@link #get(Matcher, Predicate, Function)} in
     * where it uses default parameters to extract all matches. This call is
     * equivalent to {@code get(MATCHER, Matcher::find, Matcher::group))}.
     *
     * @param regex The regex to extract.
     * @param s The string to extract from.
     * @return All matches.
     */
    public static List<String> getAll(@NonNull final String regex,
                                      @NonNull final String s) {
        return getAll(
                Pattern.compile(regex).matcher(s),
                Matcher::find,
                Matcher::group
        );
    }

    /**
     * Attempts to find a single instance of the provided regex in the provided
     * string.
     *
     * @param s The string to search.
     * @param regex The regex to look for.
     * @return {@code true} iff the provided string contains at least one match.
     */
    public static boolean contains(final String s,
                                   final String regex) {
        return matcherOf(regex, s).find();
    }

    /**
     * Takes the input arguments as build parameters to a Path, that is, iff an
     * object has the File declared type then the absolute path is appended
     * followed by "/" and any other arguments.
     *
     * @param elems The arguments to build with.
     * @return A path from all the provided elements, note that this does not
     * validate if said path is valid or functional.
     */
    public static String buildPath(final Object... elems) {
        final StringJoiner sj = new StringJoiner("/");

        for (final Object o : elems) {
            if (o instanceof File) {
                sj.add(((File) o).getAbsolutePath());
            } else {
                sj.add(o.toString());
            }
        }

        return sj.toString();
    }

    public static String toAscii(final String str) {
        return new String(
                str.getBytes(StandardCharsets.US_ASCII),
                StandardCharsets.US_ASCII
        );
    }

    /**
     * Gets a safe filename from the provided string.
     *
     * @param s The filename (extension included) to test.
     * @return MD5Hex if the filename is improper, else 's'.
     */
    public static String toFileName(final String s) {
        if (contains(s, Constants.ILLEGAL_FILE_CHARS.pattern())) {
            final String ext = getFileExtension(s, true);
            return DigestUtils.md5Hex(getFileName(s)) + ext;
        } else {
            return s;
        }
    }
}
