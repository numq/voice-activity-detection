package com.github.numq.vad.silero

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing.calculateChunkSize
import com.github.numq.vad.audio.AudioProcessing.downmixToMono
import com.github.numq.vad.audio.AudioProcessing.resample
import com.github.numq.vad.silero.model.SileroOnnxModel

internal class SileroVoiceActivityDetection(
    private val model: SileroOnnxModel,
    private val threshold: Float,
) : VoiceActivityDetection.Silero {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 32
    }

    override fun inputSizeForMillis(sampleRate: Int, channels: Int, millis: Long) = runCatching {
        val minSize = minimumInputSize(sampleRate, channels).getOrThrow()

        val factor = (millis + MINIMUM_CHUNK_MILLIS - 1) / MINIMUM_CHUNK_MILLIS

        (factor * minSize).toInt()
    }

    override fun minimumInputSize(sampleRate: Int, channels: Int) = runCatching {
        calculateChunkSize(sampleRate = sampleRate, channels = channels, millis = MINIMUM_CHUNK_MILLIS)
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching byteArrayOf()

        val chunkSize = calculateChunkSize(
            sampleRate = sampleRate,
            channels = channels,
            millis = MINIMUM_CHUNK_MILLIS
        )

        pcmBytes.asSequence().chunked(chunkSize).mapNotNull { chunk ->
            val monoChunk = downmixToMono(chunk.toByteArray(), channels)

            val resampledChunk = resample(monoChunk, sampleRate, VoiceActivityDetection.SAMPLE_RATE)

            val paddingChunkSize = calculateChunkSize(
                sampleRate = VoiceActivityDetection.SAMPLE_RATE,
                channels = VoiceActivityDetection.CHANNELS,
                millis = MINIMUM_CHUNK_MILLIS
            )

            val paddedResampledChunk = resampledChunk.copyOf(paddingChunkSize)

            val floatSamples = FloatArray(paddedResampledChunk.size / 2) { i ->
                ((paddedResampledChunk[i * 2].toInt() and 0xFF) or (paddedResampledChunk[i * 2 + 1].toInt() shl 8)) / 32767f
            }

            chunk.takeIf {
                model.process(arrayOf(floatSamples)).getOrThrow().firstOrNull()?.let { it >= threshold } == true
            }?.toByteArray()?.copyOf(chunkSize)
        }.fold(byteArrayOf()) { acc, chunk -> acc + chunk }
    }

    override fun reset() = model.reset()

    override fun close() = runCatching {
        reset().getOrDefault(Unit)
        model.close()
    }.getOrDefault(Unit)
}