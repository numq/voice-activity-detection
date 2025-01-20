package com.github.numq.vad.fvad

import com.github.numq.vad.exception.NativeException
import java.lang.ref.Cleaner

internal class NativeFvadVoiceActivityDetection : AutoCloseable {
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

    fun setMode(mode: Int): Int {
        try {
            return setModeNative(handle = nativeHandle, mode = mode)
        } catch (e: Exception) {
            throw NativeException(e)
        }
    }

    fun process(pcmBytes: ByteArray): Int {
        try {
            return processNative(handle = nativeHandle, pcmBytes = pcmBytes)
        } catch (e: Exception) {
            throw NativeException(e)
        }
    }

    fun reset() {
        try {
            return resetNative(handle = nativeHandle)
        } catch (e: Exception) {
            throw NativeException(e)
        }
    }

    override fun close() = cleanable.clean()
}
