import com.github.numq.voiceactivitydetection.VoiceActivityDetection
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SileroVoiceActivityDetectionTest {
    private val silero by lazy { VoiceActivityDetection.Silero.create().getOrThrow() }

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
        durations.forEach { duration ->
            for (channels in 1..2) {
                sampleRates.forEach { sampleRate ->
                    val pcmBytes = generateSilence(sampleRate, channels, duration)

                    assertFalse(silero.detect(pcmBytes, sampleRate, channels).getOrThrow().fragments.isNotEmpty())

                    silero.reset()
                }
            }
        }
    }

    @Test
    fun `should detect speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/short.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1

        assertTrue(silero.detect(pcmBytes, sampleRate, channels).getOrThrow().fragments.isNotEmpty())

        silero.reset()
    }

    @Test
    fun `should not detect silence in real-time`() = runTest {
        val sampleRate = sampleRates.random()
        val channels = 2

        val chunkSize = silero.minimumInputSize(sampleRate = sampleRate, channels = channels).getOrThrow()

        generateSilence(sampleRate, channels, 5.seconds).asSequence().chunked(chunkSize).map(List<Byte>::toByteArray)
            .forEach { pcmBytes ->
                assertFalse(silero.detect(pcmBytes, sampleRate, channels).getOrThrow().fragments.isNotEmpty())

                silero.reset()
            }
    }

    @Test
    fun `should detect chunked speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/long.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1

        val chunkSize = silero.minimumInputSize(sampleRate = sampleRate, channels = channels).getOrThrow()

        val results = mutableListOf<Boolean>()

        pcmBytes.asSequence().chunked(chunkSize).map(List<Byte>::toByteArray).forEach { bytes ->
            results.add(silero.detect(bytes, sampleRate, channels).getOrThrow().fragments.isNotEmpty())

            silero.reset()
        }

        assertTrue(results.contains(true))
    }
}