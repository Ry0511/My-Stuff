package com.ry.etterna.msd;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.vsrg.sequence.TimingSequence;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class created on 03/05/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public final class MinaCalc {

    // Todo I have not tested this and don't know if it can handle a wide range
    //  of files.

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
    public static native float[] getDefaultMSDFor(int[] notes, float[] times);

    /**
     * @param notes All notes to calc.
     * @param times The start time of each note, that is, zip xs zs := [(x,z)]
     * @param scoreGoal The score goal to achieve default is 0.93F.
     * @param rate The rate of the chart/notes default is 1.F.
     * @return MSD Value for the above mapped notes.
     */
    public static native float[] getMSDForRateAndGoal(int[] notes,
                                                      float[] times,
                                                      float scoreGoal,
                                                      float rate);

    /**
     * @param notes All notes to calc.
     * @param times The start time of all the notes.
     * @param fill The list to populate with MSD info.
     * @return That same list.
     */
    public static native List<float[]> getMSDForAllRates(int[] notes,
                                                         float[] times,
                                                         List<float[]> fill);

    /**
     * Causes the immediate disposal of the current CalcHandle.
     */
    public static native void dispose();

    ///////////////////////////////////////////////////////////////////////////
    // Class used as a bridge to the primitives that MinaCalc requires.
    ///////////////////////////////////////////////////////////////////////////

    // Todo this can be optimised.

    @Value
    public static class RawNotes {
        int[] notes;
        float[] times;

        public RawNotes(final EtternaNoteInfo info) {

            List<Integer> notes = new ArrayList<>();
            List<Float> times = new ArrayList<>();

            // Not fast but ehh
            final TimingSequence seq = new TimingSequence();
            info.forEachNote((m, r) -> {
                int n = r.getNoteMapping();

                if (n != 0) {
                    notes.add(n);
                    times.add(seq.getCurTimeScaled().floatValue());
                }

                seq.advanceByNote(m.size(), r.getBpm().getValue());
            });

            this.notes = new int[notes.size()];
            this.times = new float[notes.size()];
            for (int i = 0; i < notes.size(); i++) {
                this.notes[i] = notes.get(i);
                this.times[i] = times.get(i);
            }
        }
    }
}
