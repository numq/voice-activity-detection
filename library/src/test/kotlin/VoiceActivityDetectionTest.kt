import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.VoiceActivityDetectionMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.sin
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
        private const val FREQUENCY = 440.0

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

    private fun generatePcmBytes(sampleRate: Int, channels: Int, duration: Duration): ByteArray {
        val totalSamples = (sampleRate * (duration.inWholeMilliseconds / 1_000.0)).toInt()
        val pcmBytes = ByteArray(totalSamples * channels * 2)

        for (i in 0 until totalSamples) {
            val sampleValue = (sin(2 * Math.PI * FREQUENCY * i / sampleRate) * AMPLITUDE).toInt()
            for (ch in 0 until channels) {
                val index = (i * channels + ch) * 2
                pcmBytes[index] = (sampleValue and 0xFF).toByte()
                pcmBytes[index + 1] = (sampleValue shr 8 and 0xFF).toByte()
            }
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
    fun `detect speech`() {
        runDetectionTest(
            generateData = ::generatePcmBytes,
            assert = { assertTrue(it) }
        )
    }
}