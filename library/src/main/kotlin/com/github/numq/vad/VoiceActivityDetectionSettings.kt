package com.github.numq.vad

data class VoiceActivityDetectionSettings(
    val sampleRate: Int,
    val channels: Int,
    val mode: VoiceActivityDetectionMode,
    val strategy: VoiceActivityDetectionStrategy,
)