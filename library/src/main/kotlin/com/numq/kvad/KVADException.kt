package com.numq.kvad

sealed class KVADException private constructor(override val message: String) : Exception(message = message) {
    data object InvalidSampleRate : KVADException(message = "Invalid mode") {
        private fun readResolve(): Any = InvalidSampleRate
    }

    data object InvalidMode : KVADException(message = "Invalid sample rate") {
        private fun readResolve(): Any = InvalidMode
    }

    data class UnableToProcess(override val cause: Throwable?) : KVADException(message = "Unable to process bytes${
        cause?.localizedMessage?.let { message ->
            ": $message"
        } ?: ""
    }")

    data class UnableToReset(override val cause: Throwable?) : KVADException(message = "Unable to reset${
        cause?.localizedMessage?.let { message ->
            ": $message"
        } ?: ""
    }")
}