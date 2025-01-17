package com.github.numq.vad.fvad

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

internal class FvadVoiceActivityDetection(
    private val nativeVoiceActivityDetection: NativeVoiceActivityDetection,
) : VoiceActivityDetection.Fvad {
    private companion object {
        const val SAMPLE_RATE_KHZ = 8
        const val SAMPLE_RATE_HZ = SAMPLE_RATE_KHZ * 1_000
        const val CHUNK_DURATION_MILLIS = 10
        const val CHUNK_SIZE_SAMPLES = SAMPLE_RATE_KHZ * CHUNK_DURATION_MILLIS
        const val CHUNK_SIZE_BYTES = CHUNK_SIZE_SAMPLES * 2
    }

    override var mode = VoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: VoiceActivityDetectionMode) = runCatching {
        check(nativeVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching false

        val monoBytes = AudioProcessing.downmixToMono(pcmBytes, channels)

        val resampledBytes = AudioProcessing.resample(monoBytes, sampleRate, SAMPLE_RATE_HZ)

        try {
            coroutineScope {
                var isVoiceActivityDetected = false

                val jobs = resampledBytes.asSequence().chunked(CHUNK_SIZE_BYTES).mapIndexed { index, chunk ->
                    async(Dispatchers.Default) {
                        val paddedChunk = ByteArray(CHUNK_SIZE_BYTES)

                        System.arraycopy(chunk.toByteArray(), 0, paddedChunk, 0, chunk.size)

                        when (val result = nativeVoiceActivityDetection.process(paddedChunk)) {
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
        nativeVoiceActivityDetection.reset()
    }.mapCatching {
        changeMode(mode).getOrThrow()
    }.recoverCatching {
        throw IllegalStateException("Unable to reset")
    }

    override fun close() = runCatching {
        nativeVoiceActivityDetection.close()
    }.getOrDefault(Unit)
}