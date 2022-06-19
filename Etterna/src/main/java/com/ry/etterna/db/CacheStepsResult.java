package com.ry.etterna.db;

import com.ry.etterna.msd.MSD;
import com.ry.useful.old.database.Column;
import com.ry.useful.old.database.SQLiteResultMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Java class created on 11/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@NoArgsConstructor
@SQLiteResultMap(isOverrideJvm = true)
public class CacheStepsResult {

    /**
     * The smallest rate expected.
     */
    private static final BigDecimal SMALLEST_RATE = new BigDecimal("0.70");

    /**
     * The largest rate expected
     */
    private static final BigDecimal LARGEST_RATE = new BigDecimal("2.00");

    /**
     * The increment for normal rates 0.7 to 2.0 this isn't constant however I
     * don't care enough about the 0.05 factors.
     */
    private static final BigDecimal BASE_INCREMENT = new BigDecimal("0.10");

    /**
     * The name of this chart.
     */
    @Setter(onMethod_ = {@Column(value = "CHARTNAME")},
            value = AccessLevel.PRIVATE)
    private String chartName;

    /**
     * The chart key for this chart.
     */
    @Setter(onMethod_ = {@Column(value = "CHARTKEY")},
            value = AccessLevel.PRIVATE)
    private String chartKey;

    /**
     * The file for this chart.
     */
    @Setter(onMethod_ = {@Column(value = "STEPFILENAME")},
            value = AccessLevel.PRIVATE)
    private String stepFilename;

    /**
     * The MSD for all rates.
     */
    private ArrayList<MSD> msdForAllRates;

    /**
     * Loads the RAW MSD info string into a list of MSD Complex types.
     *
     * @param raw The string to process.
     */
    @Column(value = "MSD")
    private void initMSDForAllRates(final String raw) {
        final String[] allRates = raw.split(":");

        final ArrayList<MSD> pAllRates = new ArrayList<>();
        for (final String rawMsdInfo : allRates) {
            pAllRates.add(MSD.initFromStr(rawMsdInfo));
        }

        this.msdForAllRates = pAllRates;
    }

    /**
     * Attempts to find the MSD info for the provided rate.
     *
     * @param rateV The rate to search for.
     * @return Empty optional if the rate couldn't be found, else an optional of
     * what was found.
     * @implNote This only works for rates within the 0.7 and 2.0 range with
     * an increment of 0.1f.
     */
    public Optional<MSD> getMSDForRate(final String rateV) {
        final BigDecimal rate = new BigDecimal(rateV)
                .setScale(MSD.BASE_SCALE, RoundingMode.HALF_EVEN);

        // 0.7 -> 2.0 in 0.1 increments
        if (msdForAllRates.size() == MSD.BASE_INCREMENT_SIZE) {

            // Find the rate
            int i = 0;
            BigDecimal count = SMALLEST_RATE;
            while (!count.equals(rate) && !count.equals(LARGEST_RATE)) {
                count = count.add(BASE_INCREMENT, MathContext.DECIMAL64)
                        .setScale(MSD.BASE_SCALE, RoundingMode.HALF_EVEN);
                ++i;
            }

            // Return the value if inbounds (this check is redundant)
            if (this.msdForAllRates.size() > i) {
                return Optional.of(msdForAllRates.get(i));
            }

            // There can be 0.5 -> 2.0 with increments of 0.05 but will
            // ignore that for now.
        }
        return Optional.empty();
    }
}
