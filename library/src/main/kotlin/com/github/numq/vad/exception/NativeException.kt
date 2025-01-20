package com.github.numq.vad.exception

data class NativeException(override val cause: Throwable) : Exception(cause)