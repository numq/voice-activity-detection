package com.github.numq.vad.fvad

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

internal class FvadVoiceActivityDetection(
    private val nativeFvadVoiceActivityDetection: NativeFvadVoiceActivityDetection,
    private val targetSampleRate: Int,
    chunkDurationMillis: Int,
) : VoiceActivityDetection.Fvad {
    private val chunkSizeSamples = targetSampleRate / 1_000 * chunkDurationMillis

    private val chunkSizeBytes = chunkSizeSamples * 2

    private fun splitInput(input: ByteArray): Sequence<ByteArray> {
        var currentStart = 0

        return generateSequence {
            val end = minOf(currentStart + chunkSizeBytes, input.size)

            if (currentStart >= input.size) return@generateSequence null

            val chunk = input.copyOfRange(currentStart, end)

            currentStart = if (end == input.size) {
                end
            } else {
                maxOf(0, end - chunkSizeBytes / 2)
            }

            chunk
        }
    }

    override var mode = VoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: VoiceActivityDetectionMode) = runCatching {
        check(nativeFvadVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching false

        val monoBytes = AudioProcessing.downmixToMono(pcmBytes, channels)

        val resampledBytes = AudioProcessing.resample(monoBytes, sampleRate, targetSampleRate)

        try {
            /*coroutineScope {
                var isVoiceActivityDetected = false

                val jobs = splitInput(resampledBytes).asSequence().mapIndexed { index, chunk ->
                    async(Dispatchers.Default) {
                        val paddedChunk = ByteArray(chunkSizeBytes)

                        System.arraycopy(chunk, 0, paddedChunk, 0, chunk.size)

                        when (val result = nativeFvadVoiceActivityDetection.process(paddedChunk)) {
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
            }*/

            coroutineScope {
                var isVoiceActivityDetected = false

                val jobs = splitInput(pcmBytes).mapIndexed { index, chunk ->
                    async(Dispatchers.Default) {
                        val paddedChunk = ByteArray(chunkSizeBytes)

                        System.arraycopy(chunk, 0, paddedChunk, 0, chunk.size)

                        when (val result = nativeFvadVoiceActivityDetection.process(paddedChunk)) {
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