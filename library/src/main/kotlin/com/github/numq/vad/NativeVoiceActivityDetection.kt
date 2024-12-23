package com.github.numq.vad

import com.github.numq.vad.fvad.FVAD

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

        val frameSize = channels * 2
        val outputLength = pcmBytes.size / channels
        val monoBytes = ByteArray(outputLength)

        var inputIndex = 0
        var outputIndex = 0

        repeat(pcmBytes.size / frameSize) {
            var sampleSum = 0

            for (channel in 0 until channels) {
                val sample = (pcmBytes[inputIndex + channel * 2].toInt() and 0xFF) or
                        ((pcmBytes[inputIndex + channel * 2 + 1].toInt() and 0xFF) shl 8)
                sampleSum += sample
            }

            val monoSample = (sampleSum / channels).toShort()

            monoBytes[outputIndex++] = (monoSample.toInt() and 0xFF).toByte()
            monoBytes[outputIndex++] = ((monoSample.toInt() shr 8) and 0xFF).toByte()

            inputIndex += frameSize
        }

        return monoBytes
    }

    private fun resample(inputData: ByteArray, inputSampleRate: Int): ByteArray {
        require(inputSampleRate > 0) { "Input sample rate must be greater than 0" }
        require(inputData.isNotEmpty()) { "Input data must not be empty" }

        val outputLength = (inputData.size.toLong() * SAMPLE_RATE_HZ / inputSampleRate).toInt()
        require(outputLength >= 0) { "Calculated output length must not be negative" }

        val outputData = ByteArray(outputLength)
        val ratio = inputSampleRate.toFloat() / SAMPLE_RATE_HZ

        var outputIndex = 0

        for (i in 0 until outputLength / 2) {
            val srcIndex = (i * ratio).toInt()
            if (srcIndex + 1 >= inputData.size / 2) break

            val leftSample =
                (inputData[srcIndex * 2].toInt() and 0xFF) or ((inputData[srcIndex * 2 + 1].toInt() and 0xFF) shl 8)
            val rightSample =
                (inputData[(srcIndex + 1) * 2].toInt() and 0xFF) or ((inputData[(srcIndex + 1) * 2 + 1].toInt() and 0xFF) shl 8)

            val sample = ((leftSample + rightSample) / 2).toShort()

            outputData[outputIndex++] = (sample.toInt() and 0xFF).toByte()
            outputData[outputIndex++] = ((sample.toInt() shr 8) and 0xFF).toByte()
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

                val result = fvad.process(paddedChunk, CHUNK_SIZE_BYTES)

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