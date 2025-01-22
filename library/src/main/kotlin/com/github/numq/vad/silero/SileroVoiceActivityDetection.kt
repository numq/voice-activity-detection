package com.github.numq.vad.silero

import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.audio.AudioProcessing
import com.github.numq.vad.audio.AudioProcessing.calculateChunkSize
import com.github.numq.vad.audio.AudioProcessing.splitIntoChunks
import com.github.numq.vad.silero.model.OnnxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

internal class SileroVoiceActivityDetection(
    private val model: OnnxModel,
    private val threshold: Float,
) : VoiceActivityDetection.Silero {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 32
    }

    override fun minimumInputSize(sampleRate: Int, channels: Int) =
        calculateChunkSize(sampleRate = sampleRate, channels = channels, millis = MINIMUM_CHUNK_MILLIS)

    override suspend fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int) = runCatching {
        val monoBytes = AudioProcessing.downmixToMono(pcmBytes, channels)

        val resampledBytes = if (sampleRate != VoiceActivityDetection.SAMPLE_RATE) AudioProcessing.resample(
            monoBytes, sampleRate, VoiceActivityDetection.SAMPLE_RATE
        ) else pcmBytes

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
                ).map { chunk ->
                    val floatSamples = FloatArray(chunk.size / 2) { i ->
                        ((chunk[i * 2].toInt() and 0xFF) or (chunk[i * 2 + 1].toInt() shl 8)) / 32767f
                    }

                    async(Dispatchers.Default) {
                        model.process(arrayOf(floatSamples)).getOrThrow().firstOrNull()?.let { it >= threshold }
                            ?: false
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