package com.ry.useful;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.regex.Pattern;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public final class Constants {

    /**
     * Regex which matches Unsafe file system characters, such as ? or : which
     * should not be in any filename for a Windows OS.
     */
    public static final Pattern ILLEGAL_FILE_CHARS
            = Pattern.compile("[\\\\/?:*\"<>|]+");

    /**
     * Maximum Integer value.
     */
    public static final BigInteger MAX_INT
            = BigInteger.valueOf(Integer.MAX_VALUE);

    /**
     * Minimum Integer value.
     */
    public static final BigInteger MIN_INT
            = BigInteger.valueOf(Integer.MIN_VALUE);

    /**
     * Max Decimal, Double value.
     */
    public static final BigDecimal MAX_DOUBLE
            = BigDecimal.valueOf(Double.MAX_VALUE);

    /**
     * Min Decimal, Double value.
     */
    public static final BigDecimal MIN_DOUBLE
            = BigDecimal.valueOf(Double.MIN_VALUE);

    /**
     * Regular expression which matches parsable Decimal values, that is, any
     * subject string that satisfies is compatible with {@code BigDecimal}.
     */
    public static final Pattern DECIMAL_REGEX
            = Pattern.compile("(0|[1-9]\\d*)(\\.\\d+)?");

    public static BigDecimal factorDecimal(BigDecimal x, int factor) {
        return x.multiply(BigDecimal.valueOf(factor), MathContext.DECIMAL64);
    }
}
