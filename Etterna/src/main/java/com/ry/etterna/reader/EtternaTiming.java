package com.ry.etterna.reader;

import com.ry.useful.MutatingValue;
import com.ry.useful.StringUtils;
import com.ry.vsrg.BPM;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Getter
@ToString
@EqualsAndHashCode
public class EtternaTiming {

    /**
     * Regular expression to match the BPM key value pair strings of an Etterna
     * file. The below correctly matches string such as:
     * <ol>
     *     <li>0=0</li> <li>0.0=0.0</li> <li>0.00001=10.321</li>
     * </ol>
     * <p>
     * Note that spaces aren't evaluated so things like '0 = 0' won't match
     * however it is trivial to make a subject string satisfy those conditions.
     */
    private static final String ETT_BPM_REGEX
            = "(0|[1-9]\\d*)(\\.\\d+)?=(0|[1-9]\\d*)(\\.\\d+)?";

    /**
     * List of all BPM's that have been processed.
     */
    private final List<BPM> bpms;

    /**
     * Takes the input string as the arguments to build an Etterna Timing
     * object.
     *
     * @param s The string to process and map.
     * @return Etterna timing or null if no timing data was found/was corrupted.
     */
    public static EtternaTiming loadFromStr(final String s) {
        return MutatingValue.of(s)
                // Remove excess
                .mutateValue(v -> v.replaceAll("[^0-9-,.=]+", ""))
                .mutateType(v -> StringUtils.getAll(ETT_BPM_REGEX, v))

                .mutateState(xs -> xs.removeIf(x -> !x.matches(ETT_BPM_REGEX)))
                .mutateType(xs -> xs.toArray(new String[0]))
                // Map
                .mutateType(EtternaTiming::etternaTimingStrToBPM)
                .mutateType(xs -> xs.isEmpty() ? null : new EtternaTiming(xs))
                .getValue();
    }

    /**
     * Converts an Etterna Timing string to a list of individual BPMs. That is,
     * the input is expected to be the comma delimited list of key value pairs
     * 'T=BPM'.
     *
     * @param etternaTiming Array key value pairs as specified above, T=BPM.
     * @return Parsed BPM String.
     */
    private static List<BPM> etternaTimingStrToBPM(
            @NonNull final String[] etternaTiming) {
        final List<BPM> xs = new ArrayList<>();

        // X is of the format: '123=302'
        for (final String keyPairV : etternaTiming) {
            final String[] keyPairVS = keyPairV.split("[=]+");

            // Process index values
            xs.add(new BPM(
                    new BigDecimal(keyPairVS[0], MathContext.DECIMAL64),
                    new BigDecimal(keyPairVS[1], MathContext.DECIMAL64)
            ));
        }

        return xs;
    }

    /**
     * Constructs the etterna timing from BPM Values.
     *
     * @param bpmList The BPM timing values.
     */
    public EtternaTiming(final List<BPM> bpmList) {
        this.bpms = bpmList;
        // This just ensures that the bpms are ordered by start time
        this.bpms.sort(Comparator.comparing(BPM::getStartTime));
    }

    /**
     * Gets the closest BPM that does not start after the provided time.
     *
     * @param curTime The aforementioned time.
     * @return The closes BPM to the specified time that is not greater than the
     * provided time.
     */
    public BPM getLatest(final BigDecimal curTime) {
        for (int i = bpms.size(); i --> 0; ) {
            final BPM cur = this.bpms.get(i);

            // todo This might not be correct
            final BigDecimal inter = cur.getStartTime().setScale(2,
                    RoundingMode.FLOOR
            );
//            if (cur.getStartTime().compareTo(curTime) <= 0) {
            if (inter.compareTo(curTime) <= 0) {
                return cur;
            }
        }

        // Could default to 0th index but eh.
        throw new IllegalStateException(String.format(
                "Unknown BPM For time: [%s], [%s]%n",
                curTime,
                toString()
        ));
    }

    /**
     * @return Stream consisting of all BPM values, in start time order.
     */
    public Stream<BPM> stream() {
        return bpms.stream();
    }

    /**
     * Maps this Timing information to a rated timing information at the
     * specified rate from the current.
     *
     * @param rate The rate.
     * @return New instance with the uprated information.
     */
    public EtternaTiming rated(@NonNull final BigDecimal rate) {
        final List<BPM> xs = new ArrayList<>();
        bpms.forEach(x -> xs.add(x.rated(rate)));
        return new EtternaTiming(xs);
    }
}
