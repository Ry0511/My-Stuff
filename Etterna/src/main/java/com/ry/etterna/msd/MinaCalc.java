package com.ry.etterna.msd;

import com.ry.etterna.note.EtternaNoteInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Java class created on 03/05/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public final class MinaCalc {

    // Loads the MinaDll file.
    static {
        System.loadLibrary("MinaCalcNative/MinaCalc-Native-FNUtils/cmake-build-debug/MinaCalc_Native_FNUtils");
    }

    /**
     * The default score goal used for MSD.
     */
    public static final float DEFAULT_SCORE_GOAL = 0.93F;

    /**
     * The default normal rate for each file.
     */
    public static final float DEFAULT_RATE = 1.F;

    ///////////////////////////////////////////////////////////////////////////
    // Statically access Mina calc, this will assume you don't want to create
    // a calculator and map many notes but just a single set of notes.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param notes All notes to test.
     * @param times The start time of each note, that is, zip xs zs := [(x,z)]
     * @return MSD Value for the above mapped notes.
     */
    private static native float[] getDefaultMSDFor(int[] notes, float[] times);

    /**
     * @param notes All notes to calc.
     * @param times The start time of each note, that is, zip xs zs := [(x,z)]
     * @param scoreGoal The score goal to achieve default is 0.93F.
     * @param rate The rate of the chart/notes default is 1.F.
     * @return MSD Value for the above mapped notes.
     */
    private static native float[] getMSDForRateAndGoal(int[] notes,
                                                       float[] times,
                                                       float scoreGoal,
                                                       float rate);

    /**
     * @param notes All notes to calc.
     * @param times The start time of all the notes.
     * @param fill The list to populate with MSD info.
     * @return That same list.
     */
    private static native List<float[]> getMSDForAllRates(int[] notes,
                                                          float[] times,
                                                          List<float[]> fill);

    /**
     * Causes the immediate disposal of the CalcHandle.
     */
    public static native void dispose();

    /**
     * Checks to see if the dependencies for the MinaCalc have been natively
     * loaded correctly and are currently usable.
     *
     * @return If the handle can be used.
     */
    public static native boolean isNativelyLoaded();

    ///////////////////////////////////////////////////////////////////////////
    // Class used as a bridge to the primitives that MinaCalc requires.
    ///////////////////////////////////////////////////////////////////////////

    private static final class RawNotes {
        private final int[] notes;
        private final float[] times;

        RawNotes(final EtternaNoteInfo info) {
            final int size = info.getNumRows();
            this.notes = new int[size];
            this.times = new float[size];
            // Not safe but ehh
            int[] c = {0};
            info.forEachNote((m, r) -> {
                this.notes[c[0]] = r.getNoteMapping();
                this.times[c[0]] = r.getStartTime().floatValue();
                c[0] = c[0]++;
            });
        }
    }

    public static void main(String[] args) {

        int[] notes = {1, 1, 1, 1};
        float[] times = {0.F, 0.333F, 0.666F, 0.999F};

        List<float[]> allRates = getMSDForAllRates(
                notes,
                times,
                new ArrayList<>()
        );

        allRates.forEach(x -> {
            System.out.println(Arrays.toString(x));
        });
    }
}
