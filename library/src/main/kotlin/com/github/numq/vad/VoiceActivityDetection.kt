package com.github.numq.vad

import com.github.numq.vad.fvad.FvadVoiceActivityDetection
import com.github.numq.vad.fvad.NativeFvadVoiceActivityDetection
import com.github.numq.vad.fvad.VoiceActivityDetectionMode
import com.github.numq.vad.silero.SileroVoiceActivityDetection
import com.github.numq.vad.silero.model.DefaultSileroOnnxModel
import java.io.File

interface VoiceActivityDetection : AutoCloseable {
    companion object {
        const val SAMPLE_RATE = 8_000
        const val CHANNELS = 1
    }

    /**
     * Returns the minimum effective chunk size - the size at which there is no need to fill the input data with silence.
     *
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param channels the number of audio channels.
     * @return a [Result] containing the minimum chunk size in bytes.
     */
    fun minimumInputSize(sampleRate: Int, channels: Int): Result<Int>

    /**
     * Detects voice activity in the given PCM audio data.
     * The input data is split into chunks and padded with silence if needed.
     *
     * To maximize efficiency and avoid padding, ensure that the input data size is equal to [minimumInputSize].
     *
     * @param pcmBytes the audio data in PCM format.
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param channels the number of audio channels (e.g., 1 for mono, 2 for stereo).
     * @return a [Result] containing the filtered input data, which contains only recognized speech.
     */
    suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int): Result<ByteArray>

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

                FvadVoiceActivityDetection(nativeFvadVoiceActivityDetection = NativeFvadVoiceActivityDetection())
            }
        }
    }

    interface Silero : VoiceActivityDetection {
        companion object {
            /**
             * Creates a new instance of [VoiceActivityDetection].
             *
             * @param threshold the voice activity detection threshold.
             * @return a [Result] containing the created instance if successful.
             */
            fun create(threshold: Float = .5f): Result<Silero> = runCatching {
                val resourceStream = Companion::class.java.classLoader.getResourceAsStream("model/silero_vad.onnx")
                    ?: throw IllegalStateException("Model file 'silero_vad.onnx' not found in resources")

                val tempFile = File.createTempFile("silero_vad", ".onnx").apply {
                    deleteOnExit()
                    outputStream().use { resourceStream.copyTo(it) }
                }

                SileroVoiceActivityDetection(
                    model = DefaultSileroOnnxModel(
                        modelPath = tempFile.absolutePath,
                        targetSampleRate = SAMPLE_RATE
                    ),
                    threshold = threshold.coerceIn(0f, 1f)
                )
            }
        }
    }
}