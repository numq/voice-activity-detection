package playback

import device.Device

interface PlaybackService : AutoCloseable {
    suspend fun write(pcmBytes: ByteArray): Result<Unit>
    suspend fun play(device: Device): Result<Unit>
}