package com.github.numq.voiceactivitydetection.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object AudioProcessing {
    fun downmixToMono(inputData: ByteArray, channels: Int): ByteArray {
        require(inputData.isNotEmpty()) { "Input data must not be empty" }

        require(inputData.size % (channels * 2) == 0) { "PCM byte size must be a multiple of the frame size (channels * 2)" }

        require(channels > 0) { "Number of channels must be greater than 0" }

        if (channels == 1) return inputData

        val monoBytes = ByteArray(inputData.size / channels)
        val inputBuffer = ByteBuffer.wrap(inputData).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.wrap(monoBytes).order(ByteOrder.LITTLE_ENDIAN)

        val shortMin = Short.MIN_VALUE.toInt()
        val shortMax = Short.MAX_VALUE.toInt()

        while (inputBuffer.remaining() >= channels * 2) {
            var sampleSum = 0

            for (channel in 0 until channels) {
                sampleSum += inputBuffer.short.toInt()
            }

            val monoSample = (sampleSum / channels).coerceIn(shortMin, shortMax).toShort()
            outputBuffer.putShort(monoSample)
        }

        return monoBytes
    }

    fun resample(inputData: ByteArray, inputSampleRate: Int, outputSampleRate: Int, channels: Int): ByteArray {
        require(inputData.isNotEmpty()) { "Input data must not be empty" }

        require(inputSampleRate > 0) { "Input sample rate must be greater than 0" }

        require(outputSampleRate > 0) { "Output sample rate must be greater than 0" }

        require(channels > 0) { "Number of channels must be greater than 0" }

        val inputBuffer = ByteBuffer.wrap(inputData).order(ByteOrder.LITTLE_ENDIAN)
        val inputSampleCount = inputData.size / (channels * 2)
        val outputSampleCount = ((inputSampleCount.toLong() * outputSampleRate) / inputSampleRate.toDouble()).toInt()

        val outputData = ByteArray(outputSampleCount * channels * 2)
        val outputBuffer = ByteBuffer.wrap(outputData).order(ByteOrder.LITTLE_ENDIAN)

        val step = inputSampleRate.toDouble() / outputSampleRate
        var inputIndex = 0.0

        for (i in 0 until outputSampleCount) {
            val srcIndex = inputIndex.toInt()
            val fraction = inputIndex - srcIndex

            for (ch in 0 until channels) {
                val sampleOffset = srcIndex * channels + ch
                val nextSampleOffset = (srcIndex + 1) * channels + ch

                val currentSample = if (sampleOffset < inputSampleCount) {
                    inputBuffer.getShort(sampleOffset * 2).toInt()
                } else 0
                val nextSample = if (nextSampleOffset < inputSampleCount) {
                    inputBuffer.getShort(nextSampleOffset * 2).toInt()
                } else currentSample

                val interpolatedSample = (currentSample + fraction * (nextSample - currentSample)).toInt().toShort()

                outputBuffer.putShort(interpolatedSample)
            }

            inputIndex += step
        }

        return outputData
    }

    fun calculateChunkSize(sampleRate: Int, channels: Int, millis: Int): Int {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }

        require(channels > 0) { "Number of channels must be greater than 0" }

        return (((sampleRate * millis) / 1000) * 2 * channels + 3) and -4
    }
}