package com.github.numq.vad.fvad

import java.lang.ref.Cleaner

internal class FVAD : AutoCloseable {
    private val nativeHandle = initNative().also { handle ->
        require(handle != -1L) { "Unable to initialize FVAD" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    companion object {
        private val cleaner = Cleaner.create()

        @JvmStatic
        private external fun initNative(): Long

        @JvmStatic
        private external fun setModeNative(handle: Long, mode: Int): Int

        @JvmStatic
        private external fun processNative(handle: Long, pcmBytes: ByteArray): Int

        @JvmStatic
        private external fun resetNative(handle: Long)

        @JvmStatic
        private external fun freeNative(handle: Long)
    }

    fun setMode(mode: Int) = setModeNative(handle = nativeHandle, mode = mode)

    fun process(pcmBytes: ByteArray) = processNative(handle = nativeHandle, pcmBytes = pcmBytes)

    fun reset() = resetNative(handle = nativeHandle)

    override fun close() = cleanable.clean()
}
