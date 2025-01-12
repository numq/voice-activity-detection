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

        private val sampleRates = arrayOf(
            4_000,
            8_000,
            32_000,
            44_100,
            48_000,
            88_200,
            96_000,
            176_400,
            192_000
        )

        private const val AMPLITUDE = 32767 * 0.5

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

    private fun generateNoise(sampleRate: Int, channels: Int, duration: Duration): ByteArray {
        val totalSamples = (sampleRate * (duration.inWholeMilliseconds / 1_000.0)).toInt()
        val pcmBytes = ByteArray(totalSamples * channels * 2)
        for (i in pcmBytes.indices step 2) {
            val randomSample = ((Math.random() - 0.5) * 2 * AMPLITUDE).toInt()
            pcmBytes[i] = (randomSample and 0xFF).toByte()
            pcmBytes[i + 1] = (randomSample shr 8 and 0xFF).toByte()
        }
        return pcmBytes
    }

    private fun runDetectionTest(
        generateData: (Int, Int, Duration) -> ByteArray,
        assert: (Boolean) -> Unit,
    ) = runTest {
        val measurements = mutableListOf<Duration>()

        VoiceActivityDetectionMode.entries.forEach { mode ->
            voiceActivityDetection.changeMode(mode).getOrThrow()

            durations.forEach { duration ->
                for (channels in 1..2) {
                    sampleRates.forEach { sampleRate ->
                        val pcmBytes = generateData(sampleRate, channels, duration)

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
    fun `detect no speech`() {
        runDetectionTest(
            generateData = ::generateSilence,
            assert = { assertFalse(it) }
        )
    }

    @Test
    fun `detect noise`() {
        runDetectionTest(
            generateData = ::generateNoise,
            assert = { assertTrue(it) }
        )
    }
}