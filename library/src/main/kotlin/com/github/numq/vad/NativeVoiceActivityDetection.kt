package com.github.numq.vad

import com.github.numq.vad.fvad.FVAD
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class NativeVoiceActivityDetection(
    private val fvad: FVAD,
) : VoiceActivityDetection {
    private companion object {
        const val SAMPLE_RATE_KHZ = 8
        const val SAMPLE_RATE_HZ = SAMPLE_RATE_KHZ * 1_000
        const val CHUNK_DURATION_MILLIS = 10
        const val CHUNK_SIZE_SAMPLES = SAMPLE_RATE_KHZ * CHUNK_DURATION_MILLIS
        const val CHUNK_SIZE_BYTES = CHUNK_SIZE_SAMPLES * 2
    }

    override var mode = VoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: VoiceActivityDetectionMode) = runCatching {
        check(fvad.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@NativeVoiceActivityDetection.mode = mode
    }

    private fun downmixToMono(pcmBytes: ByteArray, channels: Int): ByteArray {
        require(channels > 0) { "Number of channels must be greater than 0" }
        require(pcmBytes.size % (channels * 2) == 0) { "PCM byte size must be a multiple of the frame size (channels * 2)" }

        val monoBytes = ByteArray(pcmBytes.size / channels)
        val inputBuffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.wrap(monoBytes).order(ByteOrder.LITTLE_ENDIAN)

        val shortMin = Short.MIN_VALUE.toLong()
        val shortMax = Short.MAX_VALUE.toLong()

        while (inputBuffer.remaining() >= channels * 2) {
            var sampleSum = 0L
            for (channel in 0 until channels) {
                sampleSum += inputBuffer.short.toLong()
            }
            val monoSample = (sampleSum / channels).coerceIn(shortMin, shortMax).toShort()
            outputBuffer.putShort(monoSample)
        }

        return monoBytes
    }

    private fun resample(inputData: ByteArray, inputSampleRate: Int): ByteArray {
        require(inputSampleRate > 0) { "Input sample rate must be greater than 0" }
        require(inputData.isNotEmpty()) { "Input data must not be empty" }

        val inputBuffer = ByteBuffer.wrap(inputData).order(ByteOrder.LITTLE_ENDIAN)
        val inputSampleCount = inputData.size / 2
        val outputSampleCount = ((inputSampleCount.toLong() * SAMPLE_RATE_HZ) / inputSampleRate.toDouble()).toInt()

        require(outputSampleCount >= 0) { "Output sample count must not be negative" }

        val outputData = ByteArray(outputSampleCount * 2)
        val outputBuffer = ByteBuffer.wrap(outputData).order(ByteOrder.LITTLE_ENDIAN)

        val step = inputSampleRate.toDouble() / SAMPLE_RATE_HZ
        var inputIndex = 0.0

        for (i in 0 until outputSampleCount) {
            val srcIndex = inputIndex.toInt()
            val fraction = inputIndex - srcIndex

            val leftSample = if (srcIndex < inputSampleCount) inputBuffer.getShort(srcIndex * 2).toInt() else 0
            val rightSample =
                if (srcIndex + 1 < inputSampleCount) inputBuffer.getShort((srcIndex + 1) * 2).toInt() else leftSample

            val interpolatedSample = (leftSample + fraction * (rightSample - leftSample)).toInt().toShort()
            outputBuffer.putShort(interpolatedSample)

            inputIndex += step
        }

        return outputData
    }

    override fun detect(pcmBytes: ByteArray, sampleRate: Int, channels: Int, cutOff: Boolean) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        if (pcmBytes.isEmpty()) return@runCatching false

        val resampledBytes = resample(downmixToMono(pcmBytes, channels), sampleRate)

        var isSpeechDetected = false

        resampledBytes.asSequence()
            .chunked(CHUNK_SIZE_BYTES)
            .filter { !cutOff || it.size == CHUNK_SIZE_BYTES }
            .forEachIndexed { index, chunk ->
                val paddedChunk = ByteArray(CHUNK_SIZE_BYTES)

                System.arraycopy(chunk.toByteArray(), 0, paddedChunk, 0, chunk.size)

                val result = fvad.process(paddedChunk)

                when (result) {
                    -1 -> throw IllegalStateException("Unable to process input at chunk $index")

                    1 -> {
                        isSpeechDetected = true

                        return@forEachIndexed
                    }
                }
            }

        isSpeechDetected
    }

    override fun reset() = runCatching {
        fvad.reset()
    }.mapCatching {
        changeMode(mode).getOrThrow()
    }.recoverCatching {
        throw IllegalStateException("Unable to reset")
    }

    override fun close() = runCatching {
        fvad.close()
    }.getOrDefault(Unit)
}