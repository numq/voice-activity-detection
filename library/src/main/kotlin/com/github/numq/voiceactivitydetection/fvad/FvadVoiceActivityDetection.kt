package com.github.numq.voiceactivitydetection.fvad

import com.github.numq.voiceactivitydetection.DetectedSpeech
import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.calculateChunkSize
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.downmixToMono
import com.github.numq.voiceactivitydetection.audio.AudioProcessing.resample
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

internal class FvadVoiceActivityDetection(
    private val nativeFvadVoiceActivityDetection: NativeFvadVoiceActivityDetection,
    private val minSpeechDurationMillis: Long,
    private val minSilenceDurationMillis: Long,
) : VoiceActivityDetection.Fvad {
    private companion object {
        const val MINIMUM_CHUNK_MILLIS = 30
        const val CHANNELS_MONO = 1
    }

    override fun inputSizeForMillis(channels: Int, sampleRate: Int, millis: Long) = runCatching {
        val minSize = minimumInputSize(channels = channels, sampleRate = sampleRate).getOrThrow()

        val factor = (millis + MINIMUM_CHUNK_MILLIS - 1) / MINIMUM_CHUNK_MILLIS

        (factor * minSize).toInt()
    }

    override fun minimumInputSize(channels: Int, sampleRate: Int) = runCatching {
        calculateChunkSize(channels = channels, sampleRate = sampleRate, millis = MINIMUM_CHUNK_MILLIS)
    }

    override var mode = FvadVoiceActivityDetectionMode.QUALITY

    override fun changeMode(mode: FvadVoiceActivityDetectionMode) = runCatching {
        check(nativeFvadVoiceActivityDetection.setMode(mode.ordinal) == 0) { "Unable to set mode" }

        this@FvadVoiceActivityDetection.mode = mode
    }

    override suspend fun detect(
        pcmBytes: ByteArray,
        channels: Int,
        sampleRate: Int,
        isContinuous: Boolean,
    ) = runCatching {
        require(channels > 0) { "Channel count must be at least 1" }

        require(sampleRate > 0) { "Sample rate must be greater than 0" }

        if (pcmBytes.isEmpty()) return@runCatching emptyFlow()

        val chunkSize = calculateChunkSize(
            channels = channels,
            sampleRate = sampleRate,
            millis = MINIMUM_CHUNK_MILLIS
        )

        val minSpeechSize = calculateChunkSize(
            channels = VoiceActivityDetection.CHANNELS,
            sampleRate = VoiceActivityDetection.SAMPLE_RATE,
            millis = minSpeechDurationMillis.toInt()
        )

        val chunks = pcmBytes.asSequence().chunked(chunkSize).iterator()

        var silenceDurationMillis = 0

        var isSpeechOngoing = false

        flow {
            ByteArrayOutputStream().use { baos ->
                while (chunks.hasNext()) {
                    val chunk = chunks.next()

                    val monoChunk = downmixToMono(inputData = chunk.toByteArray(), channels = channels)

                    val resampledChunk = resample(
                        inputData = monoChunk,
                        channels = CHANNELS_MONO,
                        inputSampleRate = sampleRate,
                        outputSampleRate = VoiceActivityDetection.SAMPLE_RATE
                    )

                    val paddedChunkSize = calculateChunkSize(
                        channels = VoiceActivityDetection.CHANNELS,
                        sampleRate = VoiceActivityDetection.SAMPLE_RATE,
                        millis = MINIMUM_CHUNK_MILLIS
                    )

                    val isSpeechDetected = nativeFvadVoiceActivityDetection.process(
                        resampledChunk.copyOf(paddedChunkSize)
                    ).takeIf { it in 0..1 }?.let { it == 1 }

                    requireNotNull(isSpeechDetected) { "Failed to process input" }

                    if (isSpeechDetected) {
                        baos.write(chunk.toByteArray().copyOf(chunkSize))

                        silenceDurationMillis = 0

                        if (baos.size() >= minSpeechSize) {
                            isSpeechOngoing = true
                        }
                    } else {
                        silenceDurationMillis += MINIMUM_CHUNK_MILLIS

                        if (silenceDurationMillis >= minSilenceDurationMillis && baos.size() >= minSpeechSize) {
                            emit(DetectedSpeech.Detected.Complete(bytes = baos.toByteArray()))

                            baos.reset()

                            isSpeechOngoing = false
                        }

                        if (!isSpeechOngoing) {
                            emit(DetectedSpeech.Nothing)
                        }
                    }
                }

                if (baos.size() >= minSpeechSize) {
                    emit(
                        if (isContinuous) DetectedSpeech.Detected.Segment(baos.toByteArray())
                        else DetectedSpeech.Detected.Complete(baos.toByteArray())
                    )
                }
            }
        }
    }

    override fun reset() = runCatching {
        nativeFvadVoiceActivityDetection.reset()
    }.mapCatching {
        changeMode(mode).getOrThrow()
    }.recoverCatching {
        throw IllegalStateException("Unable to reset")
    }

    override fun close() = runCatching {
        nativeFvadVoiceActivityDetection.close()
    }.getOrDefault(Unit)
}