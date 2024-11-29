package playback

import device.Device

interface PlaybackService {
    suspend fun play(device: Device, pcmBytes: ByteArray): Result<Unit>
    suspend fun stop(): Result<Unit>
}