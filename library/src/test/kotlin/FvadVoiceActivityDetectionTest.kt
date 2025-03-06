import com.github.numq.voiceactivitydetection.DetectedSpeech
import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetectionMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FvadVoiceActivityDetectionTest {
    private val fvad by lazy {
        val pathToBinaries = this::class.java.getResource("bin")!!.file

        VoiceActivityDetection.Fvad.load(
            libfvad = "$pathToBinaries\\libfvad.dll",
            voiceActivityDetection = "$pathToBinaries\\voice-activity-detection.dll"
        ).getOrThrow()

        VoiceActivityDetection.Fvad.create().getOrThrow()
    }

    private val durations = arrayOf(
        10.milliseconds,
        20.milliseconds,
        30.milliseconds,
        50.milliseconds,
        100.milliseconds,
        500.milliseconds,
        1.seconds,
        10.seconds
    )

    private val sampleRates = arrayOf(4_000, 8_000, 32_000, 44_100, 48_000, 88_200, 96_000, 176_400, 192_000)

    private fun generateSilence(sampleRate: Int, channels: Int, duration: Duration) =
        ByteArray((sampleRate * (duration.inWholeMilliseconds / 1_000.0) * channels * 2).toInt())

    @Test
    fun `should not detect silence`() = runTest {
        FvadVoiceActivityDetectionMode.entries.forEach { mode ->
            fvad.changeMode(mode).getOrThrow()

            durations.forEach { duration ->
                for (channels in 1..2) {
                    sampleRates.forEach { sampleRate ->
                        val pcmBytes = generateSilence(
                            sampleRate = sampleRate,
                            channels = channels,
                            duration = duration
                        )

                        assertFalse(
                            fvad.detect(
                                pcmBytes = pcmBytes,
                                sampleRate = sampleRate,
                                channels = channels,
                            ).getOrThrow().toList().filterIsInstance<DetectedSpeech.Detected>().isNotEmpty()
                        )

                        fvad.reset()
                    }
                }
            }
        }
    }

    @Test
    fun `should detect speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/short.wav")!!.readBytes()
        val sampleRate = 22_050
        val channels = 1

        FvadVoiceActivityDetectionMode.entries.forEach { mode ->
            fvad.changeMode(mode).getOrThrow()

            assertTrue(
                fvad.detect(
                    pcmBytes = pcmBytes,
                    sampleRate = sampleRate,
                    channels = channels,
                ).getOrThrow().toList().filterIsInstance<DetectedSpeech.Detected>().isNotEmpty()
            )

            fvad.reset()
        }
    }

    @Test
    fun `should detect sentence fragments`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/sentences.wav")!!.readBytes()
        val sampleRate = 22_050
        val channels = 1

        fvad.changeMode(FvadVoiceActivityDetectionMode.VERY_AGGRESSIVE)

        val fragments = fvad.detect(
            pcmBytes = pcmBytes,
            sampleRate = sampleRate,
            channels = channels,
        ).getOrThrow().toList().filterIsInstance<DetectedSpeech.Detected.Complete>().size

        fvad.reset()

        assertEquals(10, fragments)
    }
}