package com.github.numq.vad

import com.github.numq.vad.fvad.FVAD

interface VoiceActivityDetection : AutoCloseable {
    /**
     * The current mode of voice activity detection.
     */
    val mode: VoiceActivityDetectionMode

    /**
     * Changes the detection mode.
     *
     * @param mode the new mode to be set.
     * @return a [Result] indicating the success or failure of the operation.
     */
    fun changeMode(mode: VoiceActivityDetectionMode): Result<Unit>

    /**
     * Processes PCM audio data to detect voice activity.
     *
     * @param pcmBytes the audio data in PCM format.
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param channels the number of audio channels (e.g., 1 for mono, 2 for stereo).
     * @param cutOff if true, truncates the data by dropping the last incomplete chunk; otherwise, fills any incomplete chunk with zeroes.
     * @return a [Result] containing true if voice activity is detected, false otherwise.
     */
    fun process(pcmBytes: ByteArray, sampleRate: Int, channels: Int, cutOff: Boolean = false): Result<Boolean>

    /**
     * Resets the voice activity detection internal state.
     *
     * @return a [Result] indicating the success or failure of the operation.
     */
    fun reset(): Result<Unit>

    companion object {
        private var isInitialized = false

        /**
         * Loads the native library.
         *
         * @param libfvad the path to the libfvad library.
         * @param libfvadjni the path to the libfvadjni library.
         * @return a [Result] indicating the success or failure of the operation.
         */
        fun load(libfvad: String, libfvadjni: String) = runCatching {
            System.load(libfvad)
            System.load(libfvadjni)
        }.onSuccess {
            isInitialized = true
        }

        /**
         * Creates a new instance of [VoiceActivityDetection].
         *
         * @return a [Result] containing the created instance if successful.
         * @throws IllegalStateException if the native libraries are not initialized or if there is an issue with the underlying native library.
         */
        fun create(): Result<VoiceActivityDetection> = runCatching {
            check(isInitialized) { "Native library is not initialized" }

            NativeVoiceActivityDetection(fvad = FVAD())
        }
    }
}