package com.numq.fvad

import java.lang.ref.Cleaner

internal class FVAD : AutoCloseable {
    private val nativeHandle = initNative().also { handle ->
        require(handle != 0L) { "Unable to initialize FVAD" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    companion object {
        private val cleaner = Cleaner.create()

        @JvmStatic
        private external fun initNative(): Long

        @JvmStatic
        private external fun setModeNative(handle: Long, mode: Int): Int

        @JvmStatic
        private external fun setSampleRateNative(handle: Long, sampleRate: Int): Int

        @JvmStatic
        private external fun processNative(handle: Long, frame: ByteArray, length: Int): Int

        @JvmStatic
        private external fun resetNative(handle: Long)

        @JvmStatic
        private external fun freeNative(handle: Long)
    }

    fun setMode(mode: Int) = setModeNative(handle = nativeHandle, mode = mode)

    fun setSampleRate(sampleRate: Int) = setSampleRateNative(handle = nativeHandle, sampleRate = sampleRate)

    fun process(frame: ByteArray, length: Int) = processNative(handle = nativeHandle, frame = frame, length = length)

    fun reset() = resetNative(handle = nativeHandle)

    override fun close() = cleanable.clean()
}
