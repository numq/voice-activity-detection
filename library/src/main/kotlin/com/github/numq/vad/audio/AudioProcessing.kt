package com.github.numq.vad.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioProcessing {
    fun downmixToMono(pcmBytes: ByteArray, channels: Int): ByteArray {
        if (channels == 1) return pcmBytes

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

    fun resample(inputData: ByteArray, inputSampleRate: Int, outputSampleRate: Int): ByteArray {
        require(inputSampleRate > 0) { "Input sample rate must be greater than 0" }

        require(inputData.isNotEmpty()) { "Input data must not be empty" }

        val inputBuffer = ByteBuffer.wrap(inputData).order(ByteOrder.LITTLE_ENDIAN)
        val inputSampleCount = inputData.size / 2
        val outputSampleCount = ((inputSampleCount.toLong() * outputSampleRate) / inputSampleRate.toDouble()).toInt()

        require(outputSampleCount >= 0) { "Output sample count must not be negative" }

        val outputData = ByteArray(outputSampleCount * 2)
        val outputBuffer = ByteBuffer.wrap(outputData).order(ByteOrder.LITTLE_ENDIAN)

        val step = inputSampleRate.toDouble() / outputSampleRate
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
}