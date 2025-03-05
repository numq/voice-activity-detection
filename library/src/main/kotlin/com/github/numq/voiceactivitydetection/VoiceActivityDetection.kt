package com.github.numq.voiceactivitydetection

import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetection
import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetectionMode
import com.github.numq.voiceactivitydetection.fvad.NativeFvadVoiceActivityDetection
import com.github.numq.voiceactivitydetection.silero.SileroVoiceActivityDetection
import com.github.numq.voiceactivitydetection.silero.model.SileroModel
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VoiceActivityDetection : AutoCloseable {
    companion object {
        private const val DEFAULT_MIN_SPEECH_DURATION_MILLIS = 250L
        private const val DEFAULT_MIN_SILENCE_DURATION_MILLIS = 100L
        const val SAMPLE_RATE = 8_000
        const val CHANNELS = 1
    }

    /**
     * Returns the minimum possible input size for the given number of milliseconds.
     *
     * @param channels the number of audio channels.
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param millis the duration in milliseconds.
     * @return a [Result] containing the minimum chunk size in bytes.
     */
    fun inputSizeForMillis(channels: Int, sampleRate: Int, millis: Long): Result<Int>

    /**
     * Returns the minimum effective chunk size - the size at which there is no need to fill the input data with silence.
     *
     * @param channels the number of audio channels.
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @return a [Result] containing the minimum chunk size in bytes.
     */
    fun minimumInputSize(channels: Int, sampleRate: Int): Result<Int>

    /**
     * Detects voice activity in the given PCM audio data.
     * The input data is split into chunks and padded with silence if needed.
     *
     * To maximize efficiency and avoid padding, ensure that the input data size is equal to [minimumInputSize].
     *
     * @param pcmBytes the audio data in PCM format.
     * @param channels the number of audio channels (e.g., 1 for mono, 2 for stereo).
     * @param sampleRate the sampling rate of the audio data in Hz.
     * @param isContinuous indicates whether a call is made for continuous input. If false, the last detected speech will never be a segment. Default is `false`.
     * @return [Result] containing a flow of [DetectedSpeech], which may be a complete speech or a segment to be buffered and merged with the next detected speech.
     */
    suspend fun detect(
        pcmBytes: ByteArray,
        channels: Int,
        sampleRate: Int,
        isContinuous: Boolean = false,
    ): Result<Flow<DetectedSpeech>>


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
             * @param minSpeechDurationMillis the voice activity detection minimum speech duration millis. Default is `250`.
             * @param minSilenceDurationMillis the voice activity detection minimum silence duration millis. Default is `100`.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not initialized or if there is an issue with the underlying native library.
             */
            fun create(
                minSpeechDurationMillis: Long = DEFAULT_MIN_SPEECH_DURATION_MILLIS,
                minSilenceDurationMillis: Long = DEFAULT_MIN_SILENCE_DURATION_MILLIS,
            ): Result<Fvad> = runCatching {
                check(isLoaded) { "Native binaries were not loaded" }

                FvadVoiceActivityDetection(
                    nativeFvadVoiceActivityDetection = NativeFvadVoiceActivityDetection(),
                    minSpeechDurationMillis = minSpeechDurationMillis,
                    minSilenceDurationMillis = minSilenceDurationMillis
                )
            }
        }
    }

    interface Silero : VoiceActivityDetection {
        companion object {
            private const val DEFAULT_THRESHOLD = .5f

            /**
             * Creates a new instance of [VoiceActivityDetection].
             *
             * @param threshold the voice activity detection threshold. Default is `0.5`.
             * @param minSpeechDurationMillis the voice activity detection minimum speech duration millis. Default is `250`.
             * @param minSilenceDurationMillis the voice activity detection minimum silence duration millis. Default is `100`.
             * @return a [Result] containing the created instance if successful.
             */
            fun create(
                threshold: Float = DEFAULT_THRESHOLD,
                minSpeechDurationMillis: Long = DEFAULT_MIN_SPEECH_DURATION_MILLIS,
                minSilenceDurationMillis: Long = DEFAULT_MIN_SILENCE_DURATION_MILLIS,
            ): Result<Silero> = runCatching {
                val resourceStream = Companion::class.java.classLoader.getResourceAsStream("model/silero_vad.onnx")
                    ?: throw IllegalStateException("Model file 'silero_vad.onnx' not found in resources")

                val tempFile = File.createTempFile("silero_vad", ".onnx").apply {
                    deleteOnExit()
                    outputStream().use { resourceStream.copyTo(it) }
                }

                SileroVoiceActivityDetection(
                    model = SileroModel.create(modelPath = tempFile.absolutePath, targetSampleRate = SAMPLE_RATE),
                    threshold = threshold.coerceIn(0f, 1f),
                    minSpeechDurationMillis = minSpeechDurationMillis,
                    minSilenceDurationMillis = minSilenceDurationMillis
                )
            }
        }
    }
}