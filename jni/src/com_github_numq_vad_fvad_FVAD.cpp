#include <iostream>
#include <shared_mutex>
#include <mutex>
#include <unordered_map>
#include <memory>
#include "fvad.h"
#include "com_github_numq_vad_fvad_FVAD.h"

static jclass exceptionClass;
static std::shared_mutex mutex;
static std::unordered_map<jlong, std::shared_ptr<Fvad>> pointers;

void handleException(JNIEnv *env, const std::string &errorMessage) {
    env->ThrowNew(exceptionClass, ("JNI ERROR: " + errorMessage).c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) {
        throw std::runtime_error("Failed to get JNI environment");
    }

    exceptionClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/RuntimeException")));
    if (exceptionClass == nullptr) {
        throw std::runtime_error("Failed to find java/lang/RuntimeException class");
    }

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) return;

    if (exceptionClass) env->DeleteGlobalRef(exceptionClass);

    pointers.clear();
}

JNIEXPORT jlong JNICALL Java_com_github_numq_vad_fvad_FVAD_initNative(JNIEnv *env, jclass thisClass) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        auto fvad = std::shared_ptr<Fvad>(fvad_new(), fvad_free);
        if (!fvad) {
            throw std::runtime_error("Failed to create FVAD instance");
        }

        auto handle = reinterpret_cast<jlong>(fvad.get());

        pointers[handle] = std::move(fvad);

        return handle;
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in initNative method: ") + e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_vad_fvad_FVAD_setModeNative(JNIEnv *env, jclass thisClass, jlong handle, jint mode) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        return fvad_set_mode(it->second.get(), mode);
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in setModeNative method: ") + e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_vad_fvad_FVAD_setSampleRateNative(JNIEnv *env, jclass thisClass, jlong handle, jint sampleRate) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        return fvad_set_sample_rate(it->second.get(), sampleRate);
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in setSampleRateNative method: ") + e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_vad_fvad_FVAD_processNative(JNIEnv *env, jclass thisClass, jlong handle, jbyteArray frame,
                                                  jint length) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        jbyte *byteArray = env->GetByteArrayElements(frame, nullptr);
        if (byteArray == nullptr) {
            throw std::runtime_error("Failed to get byte array elements");
        }

        const int16_t *pcm = reinterpret_cast<const int16_t *>(byteArray);

        jint result = fvad_process(it->second.get(), pcm, length / sizeof(int16_t));

        env->ReleaseByteArrayElements(frame, byteArray, 0);

        return result;
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in processNative method: ") + e.what());
        return -1;
    }
}

JNIEXPORT void JNICALL Java_com_github_numq_vad_fvad_FVAD_resetNative(JNIEnv *env, jclass thisClass, jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        fvad_reset(it->second.get());
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in resetNative method: ") + e.what());
    }
}

JNIEXPORT void JNICALL Java_com_github_numq_vad_fvad_FVAD_freeNative(JNIEnv *env, jclass thisClass, jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        pointers.erase(it);
    } catch (const std::exception &e) {
        handleException(env, std::string("Exception in freeNative method: ") + e.what());
    }
}