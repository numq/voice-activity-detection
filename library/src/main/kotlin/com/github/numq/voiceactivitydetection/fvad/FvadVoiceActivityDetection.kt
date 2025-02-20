package com.github.numq.voiceactivitydetection.fvad

import com.github.numq.voiceactivitydetection.DetectionResult
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

                when (nativeFvadVoiceActivityDetection.process(resampledChunk.copyOf(paddedChunkSize))) {
                    0 -> if (baos.size() > 0) {
                        fragments.add(baos.toByteArray())

                        baos.reset()

                        if (index == lastIndex) {
                            isLastFragmentComplete = false
                        }
                    }

                    1 -> {
                        baos.write(chunk.toByteArray().copyOf(chunkSize))

                        if (index == lastIndex) {
                            fragments.add(baos.toByteArray())

                            baos.reset()

                            isLastFragmentComplete = true
                        }
                    }

                    else -> error("Unable to process input at chunk $index")
                }
            }

            DetectionResult(fragments = fragments, isLastFragmentComplete = isLastFragmentComplete)
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