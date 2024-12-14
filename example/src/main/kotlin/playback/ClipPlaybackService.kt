package playback

import device.Device
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineEvent

class ClipPlaybackService : PlaybackService {
    private val mutex = Mutex()

    private var baos = ByteArrayOutputStream()

    override suspend fun write(pcmBytes: ByteArray) = mutex.withLock {
        runCatching {
            baos.write(pcmBytes)
        }
    }

    override suspend fun play(device: Device) = mutex.withLock {
        runCatching {
            if (baos.size() > 0) {
                val pcmBytes = baos.toByteArray()

                baos.reset()

                val audioFormat = with(device) {
                    AudioFormat(
                        sampleRate.toFloat(),
                        sampleSizeInBits,
                        channels,
                        isSigned,
                        isBigEndian
                    )
                }

                AudioSystem.getClip().use { clip ->
                    clip.open(audioFormat, pcmBytes, 0, pcmBytes.size)

                    CompletableDeferred<Unit>().apply {
                        clip.addLineListener { event ->
                            if (event.type == LineEvent.Type.STOP) {
                                complete(Unit)
                            }
                        }

                        clip.start()

                        await()
                    }
                }
            }
        }.onFailure { println("Playback error: ${it.message}") }
    }

    override fun close() = runBlocking {
        mutex.withLock {
            baos.close()
        }
    }
}