package com.github.numq.kvad

data class KVADInputRequirement(
    val sampleRate: KVADSampleRate,
    val millis: Int,
    val sizePerChannel: Int,
)