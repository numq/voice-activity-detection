package com.github.numq.vad

import com.github.numq.vad.fvad.FvadVoiceActivityDetection
import com.github.numq.vad.fvad.NativeVoiceActivityDetection
import com.github.numq.vad.fvad.VoiceActivityDetectionMode
import com.github.numq.vad.silero.SileroVoiceActivityDetection
import com.github.numq.vad.silero.model.SileroOnnxModel
import java.io.File

interface VoiceActivityDetection : AutoCloseable {
    /**
     * Detects voice activity in the given PCM audio data.
     *
     * @param pcmBytes the audio data in PCM format.
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param channels the number of audio channels (e.g., 1 for mono, 2 for stereo).
     * @return a [Result] containing true if voice activity is detected, false otherwise.
     */
    suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int): Result<Boolean>

    /**
     * Resets the voice activity detection internal state.
     *
     * @return a [Result] indicating the success or failure of the operation.
     */
    fun reset(): Result<Unit>

    interface Fvad : VoiceActivityDetection {
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

        companion object {
            private var isLoaded = false

            /**
             * Loads the native library.
             *
             * @param libfvad the path to the libfvad library.
             * @param libvad the path to the libvad library.
             * @return a [Result] indicating the success or failure of the operation.
             */
            fun load(libfvad: String, libvad: String) = runCatching {
                System.load(libfvad)
                System.load(libvad)
            }.onSuccess {
                isLoaded = true
            }

            /**
             * Creates a new instance of [VoiceActivityDetection].
             *
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not initialized or if there is an issue with the underlying native library.
             */
            fun create(): Result<Fvad> = runCatching {
                check(isLoaded) { "Native binaries were not loaded" }

                FvadVoiceActivityDetection(nativeVoiceActivityDetection = NativeVoiceActivityDetection())
            }
        }
    }

    interface Silero : VoiceActivityDetection {
        companion object {
            private const val SAMPLE_RATE = 8_000

            /**
             * Creates a new instance of [VoiceActivityDetection].
             *
             * @param threshold the voice activity detection threshold.
             * @return a [Result] containing the created instance if successful.
             */
            fun create(threshold: Float = .5f): Result<Silero> = runCatching {
                SileroVoiceActivityDetection(
                    model = SileroOnnxModel(
                        modelPath = File(
                            Companion::class.java.getResource("../../../../model")?.file,
                            "silero_vad.onnx"
                        ).absolutePath,
                        targetSampleRate = SAMPLE_RATE
                    ),
                    threshold = threshold.coerceIn(0f, 1f),
                    targetSampleRate = SAMPLE_RATE
                )
            }
        }
    }
}