package com.github.numq.voiceactivitydetection.silero

import com.github.numq.voiceactivitydetection.DetectedSpeech
import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.calculateChunkSize
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.downmixToMono
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.resample
import com.github.numq.voiceactivitydetection.silero.model.SileroModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

internal class SileroVoiceActivityDetection(
    private val model: SileroModel,
    private val threshold: Float,
    private val minSpeechDurationMillis: Long,
    private val minSilenceDurationMillis: Long,
) : VoiceActivityDetection.Silero {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 32
        const val CHANNELS_MONO = 1
    }

    override fun inputSizeForMillis(sampleRate: Int, channels: Int, millis: Long) = runCatching {
        val minSize = minimumInputSize(sampleRate = sampleRate, channels = channels).getOrThrow()

        val factor = (millis + MINIMUM_CHUNK_MILLIS - 1) / MINIMUM_CHUNK_MILLIS

        (factor * minSize).toInt()
    }

    override fun minimumInputSize(sampleRate: Int, channels: Int) = runCatching {
        calculateChunkSize(sampleRate = sampleRate, channels = channels, millis = MINIMUM_CHUNK_MILLIS)
    }

    override suspend fun detect(
        pcmBytes: ByteArray,
        sampleRate: Int,
        channels: Int,
        isContinuous: Boolean,
    ) = runCatching {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }

        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching emptyFlow()

        val chunkSize = calculateChunkSize(
            sampleRate = sampleRate,
            channels = channels,
            millis = MINIMUM_CHUNK_MILLIS
        )

        val minSpeechSize = calculateChunkSize(
            sampleRate = VoiceActivityDetection.SAMPLE_RATE,
            channels = VoiceActivityDetection.CHANNELS,
            millis = minSpeechDurationMillis.toInt()
        )

        val chunks = pcmBytes.asSequence().chunked(chunkSize).iterator()

        var silenceDurationMillis = 0

        var isSpeechOngoing = false

        flow {
            ByteArrayOutputStream().use { baos ->
                while (chunks.hasNext()) {
                    val chunk = chunks.next()

                    val monoChunk = downmixToMono(inputData = chunk.toByteArray(), channels = channels)

                    val resampledChunk = resample(
                        inputData = monoChunk,
                        channels = CHANNELS_MONO,
                        inputSampleRate = sampleRate,
                        outputSampleRate = VoiceActivityDetection.SAMPLE_RATE
                    )

                    val paddedChunkSize = calculateChunkSize(
                        sampleRate = VoiceActivityDetection.SAMPLE_RATE,
                        channels = VoiceActivityDetection.CHANNELS,
                        millis = MINIMUM_CHUNK_MILLIS
                    )

                    val paddedResampledChunk = resampledChunk.copyOf(paddedChunkSize)

                    val floatSamples = FloatArray(paddedResampledChunk.size / 2) { i ->
                        ((paddedResampledChunk[i * 2].toInt() and 0xFF) or (paddedResampledChunk[i * 2 + 1].toInt() shl 8)) / 32767f
                    }

                    val isSpeechDetected = model.process(arrayOf(floatSamples)).getOrThrow().firstOrNull()?.let {
                        it >= threshold
                    }

                    requireNotNull(isSpeechDetected) { "Failed to process input" }

                    if (isSpeechDetected) {
                        baos.write(chunk.toByteArray().copyOf(chunkSize))

                        silenceDurationMillis = 0

                        if (baos.size() >= minSpeechSize) {
                            isSpeechOngoing = true
                        }
                    } else {
                        silenceDurationMillis += MINIMUM_CHUNK_MILLIS

                        if (silenceDurationMillis >= minSilenceDurationMillis && baos.size() >= minSpeechSize) {
                            emit(DetectedSpeech.Detected.Complete(bytes = baos.toByteArray()))

                            baos.reset()

                            isSpeechOngoing = false
                        }

                        if (!isSpeechOngoing) {
                            emit(DetectedSpeech.Nothing)
                        }
                    }
                }

                if (baos.size() >= minSpeechSize) {
                    emit(
                        if (isContinuous) DetectedSpeech.Detected.Segment(baos.toByteArray())
                        else DetectedSpeech.Detected.Complete(baos.toByteArray())
                    )
                }
            }
        }
    }

    override fun reset() = model.reset()

    override fun close() = runCatching {
        reset().getOrDefault(Unit)
        model.close()
    }.getOrDefault(Unit)
}