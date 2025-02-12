package com.github.numq.voiceactivitydetection.exception

data class NativeException(override val cause: Throwable) : Exception(cause)