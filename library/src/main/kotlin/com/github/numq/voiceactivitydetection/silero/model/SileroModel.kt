package com.github.numq.voiceactivitydetection.silero.model

internal interface SileroModel : AutoCloseable {
    fun process(input: Array<FloatArray>): Result<FloatArray>
    fun reset(batchSize: Int = 0): Result<Unit>
}