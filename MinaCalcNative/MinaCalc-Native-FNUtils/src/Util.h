//
// Created by -Ry on 03/05/2022.
//

#ifndef MINACALC_NATIVE_FNUTILS_UTIL_H
#define MINACALC_NATIVE_FNUTILS_UTIL_H

#include <vector>
#include <iostream>
#include "jni.h"
#include "../lib/0.71.0/MinaCalc/MinaCalc.h"

struct RawNotes {
    long *notes;
    float *times;
    int size;

    RawNotes(JNIEnv *env, jintArray notes, jfloatArray times) {
        this->notes = env->GetIntArrayElements(notes, nullptr);
        this->times = env->GetFloatArrayElements(times, nullptr);
        this->size = env->GetArrayLength(notes);
    }

    [[nodiscard]] std::vector<NoteInfo> asNoteInfo() const {
        auto *pNotes = new NoteInfo[size];
        for (int i = 0; i < size; ++i) {
            pNotes[i] = {(unsigned int) this->notes[i], this->times[i]};
        }
        return std::vector<NoteInfo>{pNotes, pNotes + size};
    }
};

template<typename T>
class JList {
private:
    JNIEnv* env;
    jobject list;
    jclass cls;

public:

    JList(JNIEnv* e, jobject list) {
        this->env = e;
        this->list = list;
        this->cls = this->env->GetObjectClass(this->list);
    }

    bool add(T t) {
        jmethodID id = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");
        return env->CallObjectMethod(list, id, t);
    }
};

#endif //MINACALC_NATIVE_FNUTILS_UTIL_H
