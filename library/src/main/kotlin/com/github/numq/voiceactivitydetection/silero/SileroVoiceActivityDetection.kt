package com.github.numq.voiceactivitydetection.silero

import com.github.numq.voiceactivitydetection.DetectionResult
import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.calculateChunkSize
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.downmixToMono
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.resample
import com.github.numq.voiceactivitydetection.silero.model.SileroModel
import java.io.ByteArrayOutputStream

internal class SileroVoiceActivityDetection(
    private val model: SileroModel,
    private val threshold: Float,
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

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }

        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching DetectionResult(
            fragments = emptyList(),
            isLastFragmentComplete = true
        )

        val chunkSize = calculateChunkSize(
            sampleRate = sampleRate,
            channels = channels,
            millis = MINIMUM_CHUNK_MILLIS
        )

        var isLastFragmentComplete = false

        val chunks = pcmBytes.asSequence().chunked(chunkSize)

        val lastIndex = chunks.toList().lastIndex

        val fragments = mutableListOf<ByteArray>()

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

                val paddedResampledChunk = resampledChunk.copyOf(paddedChunkSize)

                val floatSamples = FloatArray(paddedResampledChunk.size / 2) { i ->
                    ((paddedResampledChunk[i * 2].toInt() and 0xFF) or (paddedResampledChunk[i * 2 + 1].toInt() shl 8)) / 32767f
                }

                val result = model.process(arrayOf(floatSamples)).getOrThrow().firstOrNull()?.let { it >= threshold }

                requireNotNull(result) { "Processing failed at chunk $index" }

                if (result) {
                    baos.write(chunk.toByteArray().copyOf(chunkSize))

                    if (index == lastIndex) {
                        fragments.add(baos.toByteArray())

                        baos.reset()

                        isLastFragmentComplete = true
                    }
                } else if (baos.size() > 0) {
                    fragments.add(baos.toByteArray())

                    baos.reset()

                    if (index == lastIndex) {
                        isLastFragmentComplete = false
                    }
                }
            }

            DetectionResult(fragments = fragments, isLastFragmentComplete = isLastFragmentComplete)
        }
    }

    override fun reset() = model.reset()

    override fun close() = runCatching {
        reset().getOrDefault(Unit)
        model.close()
    }.getOrDefault(Unit)
}