package com.ry.etterna.msd;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.vsrg.sequence.TimingSequence;
import lombok.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class created on 03/05/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public final class MinaCalc {

    //
    // Going to put some information here; I have tested this for memory issues
    // I couldn't find any significant, nor any issues at that, though it is possible
    // to have memory issues. I have tested this and though it doesn't give
    // perfectly accurate results it does give a number close enough to the
    // expected to just knock it off as a version difference. I don't know if
    // loading the lib through a .jar is possible/functions this will need to
    // be tested if it fails it will be immediately apparent.
    //

    // Todo I have not tested this and don't know if it can handle a wide range
    //  of files.

    private static final String LIB_NAME = "MinaCalc_Native_FNUtils.dll";

    static {
        init();
    }

    // Loads the MinaDll file.
    private static void init() {
        final URL url = MinaCalc.class.getResource(LIB_NAME);

        if (url != null) {
            try {
                System.load(url.getPath());

                // If it can't load the library then we're likely in a JAR,
                // so we need to extract the native binding library to a file
                // that can be loaded.
            } catch (final UnsatisfiedLinkError err) {
                final File output = new File(System.getProperty("user.dir") + "/MinaCalcNative.dll");

                // The ultra omega try block
                try {
                    if (!output.isFile() && output.createNewFile()) {
                        try (final FileOutputStream fos = new FileOutputStream(output)) {
                            try (final InputStream is = url.openStream()) {
                                is.transferTo(fos);
                            }
                        }
                    }
                    System.load(output.getAbsolutePath());

                    // Creating the output file failed
                } catch (final IOException ex) {
                    throw new Error("Failed to create native file: "
                            + output
                            + "; More info: "
                            + ex.getMessage());

                    // This is very unlikely, but in the case the loaded one
                    // becomes invalid then we delete and retry.
                } catch (final UnsatisfiedLinkError ex) {
                    if (!output.delete()) {
                        throw new Error("Couldn't delete old native file: " + output);
                    }
                    init();
                }
            }

            // No MinaCalc.dll to load
        } else {
            throw new Error("Failed to load MinaCalc native bindings...");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Native class calls.
    ///////////////////////////////////////////////////////////////////////////

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
     * @param times The start time of each note.
     * @return MSD Value for the above mapped notes.
     */
    public static native float[] getDefaultMSDFor(int[] notes, float[] times);

    /**
     * @param notes All notes to calc.
     * @param times The start time of each note.
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
     *
     * @deprecated Usage of this method is inherently unsafe and causes the JVM
     * to crash almost always.
     */
    @Deprecated
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
                    times.add(seq.getCurTimeScaledFloat().floatValue());
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
