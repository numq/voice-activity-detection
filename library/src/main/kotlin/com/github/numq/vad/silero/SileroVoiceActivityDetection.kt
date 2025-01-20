package com.github.numq.vad.silero

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing
import com.github.numq.vad.silero.model.OnnxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

internal class SileroVoiceActivityDetection(
    private val model: OnnxModel,
    private val threshold: Float,
    private val targetSampleRate: Int,
) : VoiceActivityDetection.Silero {
    private companion object {
        const val CHUNK_SIZE = 512
        const val OVERLAP_SIZE = 256
    }

//    private fun splitInput(floatSamples: FloatArray): Array<FloatArray> {
//        val totalChunks = ((floatSamples.size - OVERLAP_SIZE) + CHUNK_SIZE - 1) / (CHUNK_SIZE - OVERLAP_SIZE)
//        val result = Array(totalChunks) { FloatArray(CHUNK_SIZE) }
//
//        for (i in 0 until totalChunks) {
//            val start = i * (CHUNK_SIZE - OVERLAP_SIZE)
//            val end = minOf(start + CHUNK_SIZE, floatSamples.size)
//            for (j in start until end) {
//                result[i][j - start] = floatSamples[j]
//            }
//        }
//
//        return result
//    }

    private fun splitInput(input: FloatArray): Sequence<FloatArray> {
        var currentStart = 0

        return generateSequence {
            val end = minOf(currentStart + CHUNK_SIZE, input.size)

            if (currentStart >= input.size) return@generateSequence null

            val chunk = input.copyOfRange(currentStart, end)

            currentStart = if (end == input.size) {
                end
            } else {
                maxOf(0, end - CHUNK_SIZE / 2)
            }

            chunk
        }
    }

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        val monoBytes = AudioProcessing.downmixToMono(pcmBytes, channels)

        val resampledBytes = if (sampleRate != targetSampleRate) AudioProcessing.resample(
            monoBytes,
            sampleRate,
            targetSampleRate
        ) else pcmBytes

        val floatSamples = FloatArray(resampledBytes.size / 2) { i ->
            ((resampledBytes[i * 2].toInt() and 0xFF) or (resampledBytes[i * 2 + 1].toInt() shl 8)) / 32767f
        }

        try {
            coroutineScope {
                var isVoiceActivityDetected = false

                val jobs = splitInput(floatSamples).map { chunk ->
                    async(Dispatchers.Default) {
                        model.process(arrayOf(chunk)).getOrThrow().firstOrNull()?.let { it >= threshold } ?: false
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

    override fun reset() = model.reset()

    override fun close() = runCatching {
        reset().getOrDefault(Unit)
        model.close()
    }.getOrDefault(Unit)
}