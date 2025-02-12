package com.github.numq.voiceactivitydetection

import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetection
import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetectionMode
import com.github.numq.voiceactivitydetection.fvad.NativeFvadVoiceActivityDetection
import com.github.numq.voiceactivitydetection.silero.SileroVoiceActivityDetection
import com.github.numq.voiceactivitydetection.silero.model.DefaultSileroOnnxModel
import java.io.File

interface VoiceActivityDetection : AutoCloseable {
    companion object {
        const val SAMPLE_RATE = 8_000
        const val CHANNELS = 1
    }

    /**
     * Returns the minimum possible input size for the given number of milliseconds.
     *
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param channels the number of audio channels.
     * @param millis the duration in milliseconds.
     * @return a [Result] containing the minimum chunk size in bytes.
     */
    fun inputSizeForMillis(sampleRate: Int, channels: Int, millis: Long): Result<Int>

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
     * @return a [Result] containing a [DetectionResult], which includes a list of detected speech fragments
     * and a boolean indicating whether the last fragment is complete.
     */
    suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int): Result<DetectionResult>

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
        val mode: FvadVoiceActivityDetectionMode

        /**
         * Changes the detection mode.
         *
         * @param mode the new mode to be set.
         * @return a [Result] indicating the success or failure of the operation.
         */
        fun changeMode(mode: FvadVoiceActivityDetectionMode): Result<Unit>

        companion object {
            private var isLoaded = false

            /**
             * Loads the native library.
             *
             * @param libfvad the path to the `libfvad` binary.
             * @param voiceActivityDetection the path to the `voice-activity-detection` binary.
             * @return a [Result] indicating the success or failure of the operation.
             */
            fun load(libfvad: String, voiceActivityDetection: String) = runCatching {
                System.load(libfvad)
                System.load(voiceActivityDetection)
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
            private const val DEFAULT_THRESHOLD = .5f

            /**
             * Creates a new instance of [VoiceActivityDetection].
             *
             * @param threshold the voice activity detection threshold.
             * @return a [Result] containing the created instance if successful.
             */
            fun create(threshold: Float = DEFAULT_THRESHOLD): Result<Silero> = runCatching {
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