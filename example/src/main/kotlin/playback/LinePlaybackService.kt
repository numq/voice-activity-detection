package playback

import device.Device
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class LinePlaybackService : PlaybackService {
    private var sourceDataLine: SourceDataLine? = null

    override suspend fun play(device: Device, pcmBytes: ByteArray) = runCatching {
        if (sourceDataLine == null || sourceDataLine?.isOpen == false) {
            val audioFormat = with(device) {
                AudioFormat(
                    sampleRate.toFloat(),
                    sampleSizeInBits,
                    channels,
                    isSigned,
                    isBigEndian
                )
            }

            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat).apply {
                open(audioFormat)
                start()
            }
        }

        sourceDataLine?.write(pcmBytes, 0, pcmBytes.size)

        Unit
    }

    override suspend fun stop() = runCatching {
        sourceDataLine?.apply {
            stop()
            close()
        }
        sourceDataLine = null
    }
}