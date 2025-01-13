import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.VoiceActivityDetectionMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class VoiceActivityDetectionTest {
    companion object {
        private val voiceActivityDetection by lazy { VoiceActivityDetection.create().getOrThrow() }

        private val durations = arrayOf(
            10.milliseconds,
            20.milliseconds,
            30.milliseconds,
            50.milliseconds,
            100.milliseconds,
            500.milliseconds,
            1.seconds,
            10.seconds,
            30.seconds,
            1.minutes
        )

        private val sampleRates = arrayOf(4_000, 8_000, 32_000, 44_100, 48_000, 88_200, 96_000, 176_400, 192_000)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            val pathToBinaries = this::class.java.getResource("bin")?.file

            VoiceActivityDetection.load(
                libfvad = "$pathToBinaries\\libfvad.dll",
                libvad = "$pathToBinaries\\libvad.dll"
            ).getOrThrow()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            voiceActivityDetection.close()
        }
    }

    @AfterEach
    fun afterEach() {
        voiceActivityDetection.reset()
    }

    private fun generateSilence(sampleRate: Int, channels: Int, duration: Duration): ByteArray {
        val totalSamples = (sampleRate * (duration.inWholeMilliseconds / 1_000.0)).toInt()
        return ByteArray(totalSamples * channels * 2)
    }

    private fun runDetectionTest(
        loadData: (Int, Int, Duration) -> ByteArray,
        assert: (Boolean) -> Unit,
    ) = runTest {
        val measurements = mutableListOf<Duration>()

        VoiceActivityDetectionMode.entries.forEach { mode ->
            voiceActivityDetection.changeMode(mode).getOrThrow()

            durations.forEach { duration ->
                for (channels in 1..2) {
                    sampleRates.forEach { sampleRate ->
                        val pcmBytes = loadData(sampleRate, channels, duration)

                        measureTime {
                            assert(voiceActivityDetection.detect(pcmBytes, sampleRate, channels).getOrThrow())
                        }.also(measurements::add)
                    }
                }
            }
        }

        println(measurements.joinToString("\n"))
    }

    @Test
    fun `should not detect silence`() = runTest {
        runDetectionTest(loadData = ::generateSilence, assert = { assertFalse(it) })
    }

    @Test
    fun `should detect speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/test.wav")?.also { println(it.file) }!!.readBytes()

        assertTrue(voiceActivityDetection.detect(pcmBytes, 48_000, 1).getOrThrow())
    }
}