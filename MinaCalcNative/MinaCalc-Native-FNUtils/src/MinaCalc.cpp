//
// Created by -Ry on 03/05/2022.
//

#include "com_ry_etterna_msd_MinaCalc.h"
#include "Util.h"

static Calc *CALC_INSTANCE = nullptr;

/**
 * @return {@code true} if the Calc Instance exists.
 */
static bool isInitialised() {
    return CALC_INSTANCE != nullptr;
}

static void initSequence() {
    if (!isInitialised()) {
        CALC_INSTANCE = new Calc();
    }
}

/**
 * @return True if the Calc handle is ready and can accept use.
 */
jboolean Java_com_ry_etterna_msd_MinaCalc_isNativelyLoaded(JNIEnv *env, jclass caller) {
    return isInitialised();
}

/**
 * Disposes of the underlying Calc handle, this is a final resource cleanup method.
 */
void Java_com_ry_etterna_msd_MinaCalc_dispose(JNIEnv *env, jclass caller) {
    if (isInitialised()) {
        delete CALC_INSTANCE;
    }
}

/**
 * @return The provided list object populated with the MSD info for each rate.
 */
jobject Java_com_ry_etterna_msd_MinaCalc_getMSDForAllRates(JNIEnv *env,
                                                           jclass caller,
                                                           jintArray notes,
                                                           jfloatArray times,
                                                           jobject list) {
    std::cout << "Calc: 0x" << CALC_INSTANCE << std::endl;
    initSequence();
    std::cout << "Calc: 0x" << CALC_INSTANCE << std::endl;
    
    JList<jfloatArray> ls(env, list);
    ls.add(env->NewFloatArray(8));
    ls.add(env->NewFloatArray(4));

    return list;
}

/**
 * @return MSD for the provided notes, and rate using the provided, target goal.
 */
jfloatArray Java_com_ry_etterna_msd_MinaCalc_getMSDForRateAndGoal(JNIEnv *env,
                                                                  jclass caller,
                                                                  jintArray notes,
                                                                  jfloatArray times,
                                                                  jfloat scoreGoal,
                                                                  jfloat rate) {
    return nullptr;
}

/**
 * @return MSD for the default 1.0 rate and default 0.93 score goal.
 */
jfloatArray Java_com_ry_etterna_msd_MinaCalc_getDefaultMSDFor(JNIEnv *env,
                                                              jclass caller,
                                                              jintArray notes,
                                                              jfloatArray times) {
    return nullptr;
}
