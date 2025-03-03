package com.github.numq.voiceactivitydetection.fvad

import com.github.numq.voiceactivitydetection.DetectedSpeech
import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.calculateChunkSize
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.downmixToMono
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.resample
import java.io.ByteArrayOutputStream

internal class FvadVoiceActivityDetection(
    private val nativeFvadVoiceActivityDetection: NativeFvadVoiceActivityDetection,
) : VoiceActivityDetection.Fvad {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 30
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

    override var mode = FvadVoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: FvadVoiceActivityDetectionMode) = runCatching {
        check(nativeFvadVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }

        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching emptyList()

        val chunkSize = calculateChunkSize(
            sampleRate = sampleRate,
            channels = channels,
            millis = MINIMUM_CHUNK_MILLIS
        )

        var isLastFragmentComplete = false

        val chunks = pcmBytes.asSequence().chunked(chunkSize)

        val lastIndex = chunks.toList().lastIndex

        val detections = mutableListOf<DetectedSpeech>()

        ByteArrayOutputStream().use { baos ->
            chunks.forEachIndexed { index, chunk ->
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

                val isSpeechDetected = nativeFvadVoiceActivityDetection.process(resampledChunk.copyOf(paddedChunkSize))
                    .takeIf { it in 0..1 }?.let { it == 1 }

                requireNotNull(isSpeechDetected) { "Processing failed at chunk $index" }

                if (isSpeechDetected) {
                    baos.write(chunk.toByteArray().copyOf(chunkSize))

                    if (index == lastIndex) {
                        detections.add(DetectedSpeech.Segment(bytes = baos.toByteArray()))
                    }
                } else if (baos.size() > 0) {
                    detections.add(DetectedSpeech.Complete(bytes = baos.toByteArray()))

                    baos.reset()
                }
            }

            detections
        }
    }

    override fun reset() = runCatching {
        nativeFvadVoiceActivityDetection.reset()
    }.mapCatching {
        changeMode(mode).getOrThrow()
    }.recoverCatching {
        throw IllegalStateException("Unable to reset")
    }

    override fun close() = runCatching {
        nativeFvadVoiceActivityDetection.close()
    }.getOrDefault(Unit)
}