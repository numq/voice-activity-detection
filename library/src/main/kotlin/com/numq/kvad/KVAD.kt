package com.numq.kvad

import com.numq.fvad.FVAD

interface KVAD : AutoCloseable {
    val sampleRate: KVADSampleRate
    val mode: KVADMode
    fun changeSampleRate(sampleRate: KVADSampleRate): Result<KVADSampleRate>
    fun changeMode(mode: KVADMode): Result<KVADMode>
    fun isSpeechDetected(pcmBytes: ByteArray): Result<Boolean>
    fun reset(): Result<Unit>

    companion object {
        fun create(): KVAD = Default(fvad = FVAD())
    }

    private class Default(private val fvad: FVAD) : KVAD {
        override var sampleRate = KVADSampleRate.RATE_48K

        override var mode = KVADMode.QUALITY

        override fun changeSampleRate(sampleRate: KVADSampleRate) = runCatching {
            if (fvad.setSampleRate(sampleRate.ordinal) != 0) throw KVADException.InvalidSampleRate
        }.map {
            sampleRate
        }.onSuccess { this@Default.sampleRate = it }

        override fun changeMode(mode: KVADMode) = runCatching {
            if (fvad.setMode(mode.ordinal) != 0) throw KVADException.InvalidMode
        }.map {
            mode
        }.onSuccess { this@Default.mode = it }

        override fun isSpeechDetected(pcmBytes: ByteArray) = runCatching {
            when (fvad.process(pcmBytes, pcmBytes.size)) {
                0 -> false

                1 -> true

                else -> throw KVADException.UnableToProcess(null)
            }
        }.recoverCatching {
            throw KVADException.UnableToProcess(it)
        }

        override fun reset() = runCatching {
            fvad.reset()
        }.recoverCatching {
            throw KVADException.UnableToReset(it)
        }

        override fun close() = runCatching {
            fvad.close()
        }.getOrDefault(Unit)
    }
}