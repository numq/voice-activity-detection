package com.github.numq.vad

import com.github.numq.vad.fvad.FVAD

interface VoiceActivityDetection : AutoCloseable {
    val settings: VoiceActivityDetectionSettings
    fun changeSettings(settings: VoiceActivityDetectionSettings): Result<Unit>
    fun process(pcmBytes: ByteArray): Result<Boolean>
    fun reset(): Result<Unit>

    companion object {
        private var isInitialized = false

        fun load(libfvad: String, libfvadjni: String) = runCatching {
            System.load(libfvad)
            System.load(libfvadjni)
        }.onSuccess {
            isInitialized = true
        }

        fun create(settings: VoiceActivityDetectionSettings): Result<VoiceActivityDetection> = runCatching {
            check(isInitialized) { "Native library is not initialized" }

            NativeVoiceActivityDetection(initialSettings = settings, fvad = FVAD())
        }
    }
}