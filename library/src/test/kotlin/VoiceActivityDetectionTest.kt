import com.github.numq.vad.VoiceActivityDetection
import com.github.numq.vad.fvad.VoiceActivityDetectionMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class VoiceActivityDetectionTest {
    companion object {
        private lateinit var fvad: VoiceActivityDetection.Fvad

        private lateinit var silero: VoiceActivityDetection.Silero

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

        private val chunksSizes = arrayOf(128, 256, 512, 1024, 2048, 4096, 8192)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            val pathToBinaries = this::class.java.getResource("bin")!!.file

            VoiceActivityDetection.Fvad.load(
                libfvad = "$pathToBinaries\\libfvad.dll",
                libvad = "$pathToBinaries\\libvad.dll"
            ).getOrThrow()

            fvad = VoiceActivityDetection.Fvad.create().getOrThrow()

            silero = VoiceActivityDetection.Silero.create().getOrThrow()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fvad.close()
            silero.close()
        }
    }

    @AfterEach
    fun afterEach() {
        fvad.reset()
        silero.reset()
    }

    private fun generateSilence(sampleRate: Int, channels: Int, duration: Duration): ByteArray {
        val totalSamples = (sampleRate * (duration.inWholeMilliseconds / 1_000.0)).toInt()
        return ByteArray(totalSamples * channels * 2)
    }

    private fun runDetectionTest(
        voiceActivityDetection: VoiceActivityDetection,
        loadData: (Int, Int, Duration) -> ByteArray,
        assert: (Boolean) -> Unit,
    ) = runTest {
        val measurements = mutableListOf<Duration>()

        VoiceActivityDetectionMode.entries.forEach { mode ->
            if (voiceActivityDetection is VoiceActivityDetection.Fvad) {
                voiceActivityDetection.changeMode(mode).getOrThrow()
            }

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
    fun `fvad should not detect silence`() =
        runDetectionTest(voiceActivityDetection = fvad, loadData = ::generateSilence, assert = { assertFalse(it) })

    @Test
    fun `silero should not detect silence`() =
        runDetectionTest(voiceActivityDetection = silero, loadData = ::generateSilence, assert = { assertFalse(it) })

    @Test
    fun `fvad should detect speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/short.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1
        assertTrue(fvad.detect(pcmBytes, sampleRate, channels).getOrThrow())
    }

    @Test
    fun `silero should detect speech`() = runTest {
        val pcmBytes = javaClass.classLoader.getResource("audio/short.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1
        assertTrue(silero.detect(pcmBytes, sampleRate, channels).getOrThrow())
    }

    @Test
    fun `fvad should not detect silence in real-time`() = runTest(timeout = ZERO) {
        val sampleRate = sampleRates.random()
        val channels = 2
        chunksSizes.forEach { chunkSize ->
            generateSilence(sampleRate, channels, 30.seconds).asSequence().chunked(chunkSize).forEach { pcmBytes ->
                assertFalse(fvad.detect(pcmBytes.toByteArray(), sampleRate, channels).getOrThrow())
            }
        }
    }

    @Test
    fun `silero should not detect silence in real-time`() = runTest(timeout = ZERO) {
        val sampleRate = sampleRates.random()
        val channels = 2
        chunksSizes.forEach { chunkSize ->
            generateSilence(sampleRate, channels, 30.seconds).asSequence().chunked(chunkSize).forEach { pcmBytes ->
                assertFalse(fvad.detect(pcmBytes.toByteArray(), sampleRate, channels).getOrThrow())
            }
        }
    }

    @Test
    fun `fvad should detect speech in real-time`() = runTest(timeout = ZERO) {
        val pcmBytes = javaClass.classLoader.getResource("audio/long.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1
        chunksSizes.forEach { chunkSize ->
            pcmBytes.asSequence().chunked(chunkSize).forEach { bytes ->
                assertDoesNotThrow {
                    fvad.detect(bytes.toByteArray(), sampleRate, channels).getOrThrow()
                }
            }
        }
    }

    @Test
    fun `silero should detect speech in real-time`() = runTest(timeout = ZERO) {
        val pcmBytes = javaClass.classLoader.getResource("audio/long.wav")!!.readBytes()
        val sampleRate = 48_000
        val channels = 1
        chunksSizes.forEach { chunkSize ->
            pcmBytes.asSequence().chunked(chunkSize).forEach { bytes ->
                assertDoesNotThrow {
                    fvad.detect(bytes.toByteArray(), sampleRate, channels).getOrThrow()
                }
            }
        }
    }
}