package com.github.numq.kvad

data class VADInputRequirement(
    val sampleRate: VADSampleRate,
    val millis: Int,
    val sizePerChannel: Int,
)