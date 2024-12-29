package playback

interface PlaybackService : AutoCloseable {
    suspend fun write(pcmBytes: ByteArray): Result<Unit>
    suspend fun play(): Result<Unit>

    companion object {
        fun create(): Result<PlaybackService> = runCatching { ClipPlaybackService() }
    }
}