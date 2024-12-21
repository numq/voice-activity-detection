package com.github.numq.vad

import com.github.numq.vad.fvad.FVAD

interface VoiceActivityDetection : AutoCloseable {
    val mode: VoiceActivityDetectionMode
    fun changeMode(mode: VoiceActivityDetectionMode): Result<Unit>
    fun process(pcmBytes: ByteArray, sampleRate: Int, channels: Int, cutOff: Boolean = false): Result<Boolean>
    fun reset(): Result<Unit>

    companion object {
        private var isInitialized = false

        fun load(libfvad: String, libfvadjni: String) = runCatching {
            System.load(libfvad)
            System.load(libfvadjni)
        }.onSuccess {
            isInitialized = true
        }

        fun create(): Result<VoiceActivityDetection> = runCatching {
            check(isInitialized) { "Native library is not initialized" }

            NativeVoiceActivityDetection(fvad = FVAD())
        }
    }
}