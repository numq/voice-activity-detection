#include <jni.h>

#ifndef _Included_com_github_numq_vad_fvad_FVAD
#define _Included_com_github_numq_vad_fvad_FVAD
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_github_numq_vad_NativeVoiceActivityDetection_initNative
        (JNIEnv *, jclass);

JNIEXPORT jint JNICALL Java_com_github_numq_vad_NativeVoiceActivityDetection_setModeNative
        (JNIEnv *, jclass, jlong, jint);

JNIEXPORT jint JNICALL Java_com_github_numq_vad_NativeVoiceActivityDetection_processNative
        (JNIEnv *, jclass, jlong, jbyteArray);

JNIEXPORT void JNICALL Java_com_github_numq_vad_NativeVoiceActivityDetection_resetNative
        (JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL Java_com_github_numq_vad_NativeVoiceActivityDetection_freeNative
        (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
