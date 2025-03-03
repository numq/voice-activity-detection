package com.github.numq.voiceactivitydetection

sealed interface DetectedSpeech {
    val bytes: ByteArray

    data class Complete(override val bytes: ByteArray) : DetectedSpeech {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Complete

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    data class Segment(override val bytes: ByteArray) : DetectedSpeech {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Segment

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}