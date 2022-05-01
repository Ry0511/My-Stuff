package com.ry.etterna.msd;

import com.ry.useful.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class MSD {

    /**
     * A reasonable cap for MSD values, that is, any values greater than this
     * will just have this value.
     */
    public static final BigDecimal REASONABLE_LIMIT
            = new BigDecimal("65.00");

    /**
     * If MSD values don't have a mapped value, that is, not a number then this
     * is the debug value reference used.
     */
    public static final BigDecimal NAN = new BigDecimal("-1.0");

    /**
     * The number of rates expected for the base 0.1 increment system.
     */
    public static final int BASE_INCREMENT_SIZE = 14;

    /**
     * The radix scale for MSD Values.
     */
    public static final int BASE_SCALE = 2;

    /**
     * When scaling MSD Values, truncate, though half even is probably a better
     * choice however this really doesn't matter since almost no MSD values have
     * a scale exceeding 2 with a radix that actually makes a difference.
     */
    public static final RoundingMode MODE = RoundingMode.DOWN;

    /**
     * Array of MSD Skillset values.
     */
    private final BigDecimal[] msdValues;

    /**
     * Attempts to parse the provided string value into an MSD value.
     *
     * @param raw The value to parse.
     * @return Iff raw is not a number then {@link #NAN} else if raw is a number
     * then {@link #REASONABLE_LIMIT} or RAW parsed as a BigDecimal.
     */
    private static BigDecimal parseValue(final String raw) {
        if (StringUtils.isFloat(raw)) {
            return new BigDecimal(raw).min(REASONABLE_LIMIT);

            // Not a number
        } else {
            return NAN;
        }
    }

    /**
     * Parses the raw single rate MSD information into parsed values.
     *
     * @param raw Comma delimited args to process.
     * @return MSD info from the provided raw string.
     */
    public static MSD initFromStr(final String raw) {
        final String[] skillSets = raw.replaceAll("[^0-9.,]+", "").split(",");

        // Shouldn't occur unless I missed something
        if (skillSets.length != SkillSet.NUM_SKILLSETS) {
            throw new IllegalStateException(String.format(
                    "MSD Load fail, num skills to process: '%s' should be "
                            + "'%s' processed from string '%s'%n",
                    skillSets.length,
                    SkillSet.NUM_SKILLSETS,
                    raw
            ));
        }

        // Load skills + init container
        final BigDecimal[] skills = new BigDecimal[SkillSet.NUM_SKILLSETS];
        for (int i = 0; i < skills.length; ++i) {
            skills[i] = parseValue(skillSets[i]);
        }
        return new MSD(skills);
    }

    /**
     * Gets the scaled MSD value for the provided skillset.
     *
     * @param skill The skillset to get.
     * @return MSD Value scaled to 2 decimal places.
     */
    public BigDecimal getSkill(final SkillSet skill) {
        return msdValues[skill.ordinal()].setScale(BASE_SCALE, MODE);
    }

    /**
     * Gets the string representation of the provided skillsets value, that is,
     * if the skills value is NaN then a String of "NaN" is returned. Else the
     * decimal value is returned.
     *
     * @param skill The skill to get a string representation for.
     * @return NaN or a number.
     */
    public String skillToString(final SkillSet skill) {
        final BigDecimal x = getSkill(skill);
        if (x == NAN) return "NaN";
        return x.toPlainString();
    }

    /**
     * @param action The action to apply to each skill and its value.
     */
    public void forEachSkill(final BiConsumer<SkillSet, BigDecimal> action) {
        for (final SkillSet skill : SkillSet.values()) {
            action.accept(skill, getSkill(skill));
        }
    }

    /**
     * Interpolates this MSD data with another producing another MSD of which is
     * the culmination of difference between each skill. This exists to
     * calculate sandwiched rates such as 0.05 increments i.e., fn(1.0, 1.1) =
     * 1.05.
     *
     * @param other The other MSD data to interpolate between.
     * @implNote This is more of a Mean of the two than it is anything else.
     */
    public MSD interpolateMSD(final MSD other) {
        final BigDecimal[] interpolated
                = new BigDecimal[SkillSet.NUM_SKILLSETS];

        final AtomicInteger index = new AtomicInteger();
        forEachSkill((skill, value) -> {
            final BigDecimal otherValue = other.getSkill(skill);

            // (A + B)
            interpolated[index.getAndIncrement()] = value.add(
                    otherValue,
                    MathContext.DECIMAL64

                    // (A + B) / 2
            ).divide(
                    new BigDecimal("2.0"),
                    MathContext.DECIMAL64

                    // 2DP & 0.5 UP
            ).setScale(
                    BASE_SCALE,
                    RoundingMode.HALF_UP
            );
        });

        return new MSD(interpolated);
    }

    /**
     * @return All skillsets neatly formatted in a readable string.
     */
    public String debugStr() {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");
        forEachSkill((s, v) -> sj.add(s.getAcronym() + ": " + skillToString(s)));
        return sj.toString();
    }

    /**
     * Gets the provided skill if and only if the skill has a value ranged
     * between 0 and 64.99.
     *
     * @param skill The skill to get.
     * @return Empty if the value was out of the aforementioned range, else the
     * value.
     */
    public Optional<BigDecimal> getSkillIfReasonable(final SkillSet skill) {
        final BigDecimal x = getSkill(skill);
        if ((x == NAN) || (x == REASONABLE_LIMIT)) return Optional.empty();
        return Optional.of(x);
    }

    /**
     * @param skill The skill to check for the specified range.
     * @param min The minimum (inclusive).
     * @param max The maximum (inclusive).
     * @return {@code true} if the value of the skillset is min, max, or a value
     * between min and max.
     */
    public boolean inRange(final SkillSet skill,
                           final String min,
                           final String max) {
        final BigDecimal mi = new BigDecimal(min, MathContext.DECIMAL64);
        final BigDecimal ma = new BigDecimal(max, MathContext.DECIMAL64);
        final BigDecimal sk = getSkill(skill);

        // (sk >= min) && (sk <= max)
        return sk.compareTo(mi) >= 0 && sk.compareTo(ma) <= 0;
    }

    /**
     * @return {@code true} if the overall MSD is within the range 18 to 35.
     */
    public boolean inRange() {
        return inRange(SkillSet.OVERALL, "18", "35");
    }

    /**
     * @return The highest skillset which is not Overall or Stamina.
     */
    public SkillSet getBestSkill() {
        return Stream.of(SkillSet.values())
                .filter(x -> x != SkillSet.OVERALL && x != SkillSet.STAMINA)
                .max(Comparator.comparing(this::getSkill))
                .orElseThrow(RuntimeException::new);
    }

    /**
     * Creates a search tag for the Overall MSD, that is, for values mi to ma in
     * increments of inc test if MSD>v, providing the appropriate search
     * string.
     *
     * @param mi The min value (Start).
     * @param ma The max value (End).
     * @param inc The increment (V + INC).
     * @return MSD Filter tags, minimum of 2 comma delimited arguments, MSD>?
     * and SKILL!.
     */
    public String getMsdFilterTag(final String mi,
                                  final String ma,
                                  final String inc) {
        final BigDecimal overall = getSkill(SkillSet.OVERALL);
        final BigDecimal max = new BigDecimal(ma, MathContext.DECIMAL64);
        final BigDecimal increment = new BigDecimal(inc, MathContext.DECIMAL64);
        BigDecimal val = new BigDecimal(mi, MathContext.DECIMAL64);

        final StringJoiner sj = new StringJoiner(",");
        while (val.compareTo(max) <= 0) {

            // 2.12345 -> 2.12; 2.001 -> 2
            final BigDecimal clamp = val
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros();

            switch (overall.compareTo(val)) {
                case 1 -> sj.add("MSD>" + clamp.toPlainString());
                case -1 -> sj.add("MSD<" + clamp.toPlainString());
                case 0 -> sj.add("MSD==" + clamp.toPlainString());
            }

            // Increment
            val = val.add(increment, MathContext.DECIMAL64);
        }

        if (overall == NAN) sj.add("MSD==NaN");
        sj.add(getBestSkill().getAcronym() + "!");
        sj.add("MSD>?");
        return sj.toString();
    }

    /**
     * @return Skillset source string.
     */
    public String sourceStr() {
        final StringJoiner sj = new StringJoiner(", ");
        forEachSkill((s, v) -> sj.add(s.getAcronym()
                + ": "
                + skillToString(s))
        );

        return sj.toString();
    }
}
