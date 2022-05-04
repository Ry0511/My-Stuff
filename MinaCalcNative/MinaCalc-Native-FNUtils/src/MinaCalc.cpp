//
// Created by -Ry on 03/05/2022.
//

#include "com_ry_etterna_msd_MinaCalc.h"
#include "Util.h"

static const int NUM_SKILL_SETS = 8;
static const float DEFAULT_RATE = 1.F;
static const float DEFAULT_SCORE_GOAL = 0.93F;

static Calc *CALC_INSTANCE = nullptr;

/**
 * @return {@code true} if the Calc Instance exists.
 */
static bool isInitialised() {
    return CALC_INSTANCE != nullptr;
}

/**
 * Initialises the Calc handle if it currently does not exist.
 */
static void initSequence() {
    if (!isInitialised()) {
        CALC_INSTANCE = new Calc();
    }
}

/**
 * Disposes of the underlying Calc handle, this is a final resource cleanup method.
 */
void Java_com_ry_etterna_msd_MinaCalc_dispose(JNIEnv *env, jclass caller) {
    if (isInitialised()) {
        delete CALC_INSTANCE;
    }
}

//
// Note that these methods can throw assertion errors on malformed input.
// If an assertion fail happens from a native call the entire JVM dies
// I don't like this, but I have yet to find a concrete solution, which
// does not include a bunch of data validation checks.
//

/**
 * @return MSD for the default 1.0 rate and default 0.93 score goal.
 */
jfloatArray Java_com_ry_etterna_msd_MinaCalc_getDefaultMSDFor(JNIEnv *env,
                                                              jclass caller,
                                                              jintArray notes,
                                                              jfloatArray times) {
    initSequence();

    auto msd = MinaSDCalc(
            RawNotes{env, notes, times}.asNoteInfo(),
            DEFAULT_RATE,
            DEFAULT_SCORE_GOAL,
            CALC_INSTANCE
    );
    jfloatArray array = env->NewFloatArray(NUM_SKILL_SETS);
    env->SetFloatArrayRegion(array, 0, NUM_SKILL_SETS, msd.data());
    std::destroy(msd.begin(), msd.end());

    return array;
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
    initSequence();

    auto msd = MinaSDCalc(
            RawNotes{env, notes, times}.asNoteInfo(),
            rate,
            scoreGoal,
            CALC_INSTANCE
    );
    jfloatArray array = env->NewFloatArray(NUM_SKILL_SETS);
    env->SetFloatArrayRegion(array, 0, NUM_SKILL_SETS, msd.data());
    std::destroy(msd.begin(), msd.end());

    return array;
}

/**
 * @return The provided list object populated with the MSD info for each rate.
 */
jobject Java_com_ry_etterna_msd_MinaCalc_getMSDForAllRates(JNIEnv *env,
                                                           jclass caller,
                                                           jintArray notes,
                                                           jfloatArray times,
                                                           jobject list) {
    initSequence();

    auto msdForAllRates = MinaSDCalc(
            RawNotes{env, notes, times}.asNoteInfo(),
            CALC_INSTANCE
    );

    // Append results to the provided list
    JList<jfloatArray> nativeList(env, list);
    for (auto msdForRate: msdForAllRates) {
        jfloatArray array = env->NewFloatArray(NUM_SKILL_SETS);
        env->SetFloatArrayRegion(array, 0, NUM_SKILL_SETS, msdForRate.data());
        std::destroy(msdForRate.begin(), msdForRate.end());
        nativeList.add(array);
    }

    return list;
}
