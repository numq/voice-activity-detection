package com.github.numq.vad.fvad

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing.calculateChunkSize
import com.github.numq.vad.audio.AudioProcessing.downmixToMono
import com.github.numq.vad.audio.AudioProcessing.resample

internal class FvadVoiceActivityDetection(
    private val nativeFvadVoiceActivityDetection: NativeFvadVoiceActivityDetection,
) : VoiceActivityDetection.Fvad {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 10
    }

    override fun inputSizeForMillis(sampleRate: Int, channels: Int, millis: Long) = runCatching {
        val minSize = minimumInputSize(sampleRate, channels).getOrThrow()

        val factor = (millis + MINIMUM_CHUNK_MILLIS - 1) / MINIMUM_CHUNK_MILLIS

        (factor * minSize).toInt()
    }

    override fun minimumInputSize(sampleRate: Int, channels: Int) = runCatching {
        calculateChunkSize(sampleRate = sampleRate, channels = channels, millis = MINIMUM_CHUNK_MILLIS)
    }

    override var mode = VoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: VoiceActivityDetectionMode) = runCatching {
        check(nativeFvadVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching byteArrayOf()

        val chunkSize = calculateChunkSize(
            sampleRate = sampleRate,
            channels = channels,
            millis = MINIMUM_CHUNK_MILLIS
        )

        pcmBytes.asSequence().chunked(chunkSize).mapIndexedNotNull { index, chunk ->
            val monoChunk = downmixToMono(chunk.toByteArray(), channels)

            val resampledChunk = resample(monoChunk, sampleRate, VoiceActivityDetection.SAMPLE_RATE)

            val paddingChunkSize = calculateChunkSize(
                sampleRate = VoiceActivityDetection.SAMPLE_RATE,
                channels = VoiceActivityDetection.CHANNELS,
                millis = MINIMUM_CHUNK_MILLIS
            )

            when (nativeFvadVoiceActivityDetection.process(resampledChunk.copyOf(paddingChunkSize))) {
                0 -> null

                1 -> chunk.toByteArray().copyOf(chunkSize)

                else -> throw IllegalStateException("Unable to process input at chunk $index")
            }
        }.fold(byteArrayOf()) { acc, chunk -> acc + chunk }
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