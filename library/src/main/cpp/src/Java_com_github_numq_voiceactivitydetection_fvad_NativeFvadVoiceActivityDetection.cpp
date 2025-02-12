#include "Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection.h"

static jclass exceptionClass;
static std::shared_mutex mutex;
static std::unordered_map<jlong, fvad_ptr> pointers;

void handleException(JNIEnv *env, const std::string &errorMessage) {
    env->ThrowNew(exceptionClass, errorMessage.c_str());
}

Fvad *getPointer(jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    auto it = pointers.find(handle);
    if (it == pointers.end()) {
        throw std::runtime_error("Invalid handle");
    }
    return it->second.get();
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }

    exceptionClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/RuntimeException")));
    if (exceptionClass == nullptr) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) return;

    if (exceptionClass) env->DeleteGlobalRef(exceptionClass);

    pointers.clear();
}

JNIEXPORT jlong JNICALL
Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection_initNative(JNIEnv *env, jclass thisClass) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        auto fvad = fvad_new();
        if (!fvad) {
            throw std::runtime_error("Failed to create native instance");
        }

        fvad_ptr ptr(fvad);

        auto handle = reinterpret_cast<jlong>(ptr.get());

        pointers[handle] = std::move(ptr);

        return handle;
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection_setModeNative(JNIEnv *env, jclass thisClass,
                                                                             jlong handle,
                                                                             jint mode) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto fvad = getPointer(handle);

        return fvad_set_mode(fvad, mode);
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection_processNative(JNIEnv *env, jclass thisClass,
                                                                             jlong handle,
                                                                             jbyteArray pcmBytes) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto fvad = getPointer(handle);

        auto length = env->GetArrayLength(pcmBytes);
        if (length == 0) {
            throw std::runtime_error("Array is empty");
        }

        jbyte *byteArray = env->GetByteArrayElements(pcmBytes, nullptr);
        if (byteArray == nullptr) {
            throw std::runtime_error("Failed to get byte array elements");
        }

        const auto *pcm = reinterpret_cast<const int16_t *>(byteArray);

        env->ReleaseByteArrayElements(pcmBytes, byteArray, JNI_ABORT);

        jint result = fvad_process(fvad, pcm, length / sizeof(int16_t));

        return result;
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return -1;
    }
}

JNIEXPORT void JNICALL
Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection_resetNative(JNIEnv *env, jclass thisClass,
                                                                           jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto fvad = getPointer(handle);

        fvad_reset(fvad);
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}

JNIEXPORT void JNICALL
Java_com_github_numq_voiceactivitydetection_fvad_NativeFvadVoiceActivityDetection_freeNative(JNIEnv *env, jclass thisClass, jlong handle) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        if (pointers.erase(handle) == 0) {
            handleException(env, "Unable to free native pointer");
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}