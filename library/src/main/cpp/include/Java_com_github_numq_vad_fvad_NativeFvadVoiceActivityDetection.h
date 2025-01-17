#include <jni.h>
#include <iostream>
#include <shared_mutex>
#include <mutex>
#include <unordered_map>
#include <memory>
#include "fvad.h"

#ifndef _Included_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection
#define _Included_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection_initNative
        (JNIEnv *, jclass);

JNIEXPORT jint JNICALL Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection_setModeNative
        (JNIEnv *, jclass, jlong, jint);

JNIEXPORT jint JNICALL Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection_processNative
        (JNIEnv *, jclass, jlong, jbyteArray);

JNIEXPORT void JNICALL Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection_resetNative
        (JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL Java_com_github_numq_vad_fvad_NativeFvadVoiceActivityDetection_freeNative
        (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
