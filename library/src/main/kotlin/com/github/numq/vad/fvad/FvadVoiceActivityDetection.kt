package com.github.numq.vad.fvad

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing
import com.github.numq.vad.audio.AudioProcessing.calculateChunkSize
import com.github.numq.vad.audio.AudioProcessing.splitIntoChunks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

internal class FvadVoiceActivityDetection(
    private val nativeFvadVoiceActivityDetection: NativeFvadVoiceActivityDetection,
) : VoiceActivityDetection.Fvad {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 10
    }

    override fun minimumInputSize(sampleRate: Int, channels: Int) =
        calculateChunkSize(sampleRate = sampleRate, channels = channels, millis = MINIMUM_CHUNK_MILLIS)

    override var mode = VoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: VoiceActivityDetectionMode) = runCatching {
        check(nativeFvadVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching false

        val monoBytes = AudioProcessing.downmixToMono(pcmBytes, channels)

        val resampledBytes = AudioProcessing.resample(monoBytes, sampleRate, VoiceActivityDetection.SAMPLE_RATE)

        try {
            coroutineScope {
                var isVoiceActivityDetected = false

                val jobs = splitIntoChunks(
                    inputData = resampledBytes,
                    chunkSize = calculateChunkSize(
                        sampleRate = VoiceActivityDetection.SAMPLE_RATE,
                        channels = VoiceActivityDetection.CHANNELS,
                        millis = MINIMUM_CHUNK_MILLIS
                    )
                ).mapIndexed { index, chunk ->
                    async(Dispatchers.Default) {
                        when (val result = nativeFvadVoiceActivityDetection.process(chunk)) {
                            -1 -> throw IllegalStateException("Unable to process input at chunk $index")

                            else -> result == 1
                        }
                    }
                }

                jobs.forEach {
                    when {
                        isVoiceActivityDetected -> it.cancel()

                        it.await() -> isVoiceActivityDetected = true
                    }
                }

                isVoiceActivityDetected
            }
        } catch (e: CancellationException) {
            false
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