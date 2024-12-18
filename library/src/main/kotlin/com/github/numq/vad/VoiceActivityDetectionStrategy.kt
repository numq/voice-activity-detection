package com.github.numq.vad

enum class VoiceActivityDetectionStrategy(val millis: Long) {
    NORMAL(30L), FAST(20L), FASTEST(10L)
}