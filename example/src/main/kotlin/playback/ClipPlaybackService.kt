package playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineEvent

internal class ClipPlaybackService : PlaybackService {
    private val mutex = Mutex()

    private var baos = ByteArrayOutputStream()

    override suspend fun write(pcmBytes: ByteArray) = mutex.withLock {
        runCatching {
            baos.write(pcmBytes)
        }
    }

    override suspend fun play() = mutex.withLock {
        runCatching {
            if (baos.size() > 0) {
                val pcmBytes = baos.toByteArray()

                baos.reset()

                AudioSystem.getClip().use { clip ->
                    clip.open(clip.format, pcmBytes, 0, pcmBytes.size)

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

    override fun close() = runCatching { baos.close() }.getOrDefault(Unit)
}