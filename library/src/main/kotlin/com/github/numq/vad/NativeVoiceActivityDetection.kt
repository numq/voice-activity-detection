package com.github.numq.vad

import java.lang.ref.Cleaner

internal class NativeVoiceActivityDetection : AutoCloseable {
    private val nativeHandle = initNative().also { handle ->
        require(handle != -1L) { "Unable to initialize native library" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        external fun initNative(): Long

        @JvmStatic
        external fun setModeNative(handle: Long, mode: Int): Int

        @JvmStatic
        external fun processNative(handle: Long, pcmBytes: ByteArray): Int

        @JvmStatic
        external fun resetNative(handle: Long)

        @JvmStatic
        external fun freeNative(handle: Long)
    }

    fun setMode(mode: Int) = setModeNative(handle = nativeHandle, mode = mode)

    fun process(pcmBytes: ByteArray) = processNative(handle = nativeHandle, pcmBytes = pcmBytes)

    fun reset() = resetNative(handle = nativeHandle)

    override fun close() = cleanable.clean()
}
