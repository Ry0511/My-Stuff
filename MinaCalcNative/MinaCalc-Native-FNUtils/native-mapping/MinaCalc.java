package com.ry.etterna.msd;
//
// Contains only the native method declarations
//

import java.util.List;

public class MinaCalc {
    private static native float[] getDefaultMSDFor(int[] notes, float[] times);
    private static native float[] getMSDForRateAndGoal(int[] notes,float[] times,float scoreGoal,float rate);
    private static native List<float[]> getMSDForAllRates(int[] notes,float[] times,List<float[]> fill);
    public static native void dispose();
//     public static native boolean isNativelyLoaded(); // I found this to be unnecessary
}

// Create header file with: javac -h src native-mapping/MinaCalc.java