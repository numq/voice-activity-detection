#include <jni.h>

#ifndef _Included_com_github_numq_kvad_fvad_FVAD
#define _Included_com_github_numq_kvad_fvad_FVAD
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_github_numq_kvad_fvad_FVAD_initNative
        (JNIEnv *, jclass);

JNIEXPORT jint JNICALL Java_com_github_numq_kvad_fvad_FVAD_setModeNative
        (JNIEnv *, jclass, jlong, jint);

JNIEXPORT jint JNICALL Java_com_github_numq_kvad_fvad_FVAD_setSampleRateNative
        (JNIEnv *, jclass, jlong, jint);

JNIEXPORT jint JNICALL Java_com_github_numq_kvad_fvad_FVAD_processNative
        (JNIEnv *, jclass, jlong, jbyteArray, jint);

JNIEXPORT void JNICALL Java_com_github_numq_kvad_fvad_FVAD_resetNative
        (JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL Java_com_github_numq_kvad_fvad_FVAD_freeNative
        (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
