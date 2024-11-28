package capturing

import device.Device
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine

class LineCapturingService : CapturingService {
    private fun findMicrophone(device: Device) = runCatching {
        val format = with(device) {
            AudioFormat(
                sampleRate.toFloat(),
                sampleSizeInBits,
                channels,
                isSigned,
                isBigEndian
            )
        }

        val mixerInfo = AudioSystem.getMixerInfo().firstOrNull {
            it.name.equals(device.name)
        } ?: throw Exception("Device not found")

        val mixer = AudioSystem.getMixer(mixerInfo)

        val lineInfo = mixer.targetLineInfo.firstOrNull { it is DataLine.Info }
            ?: throw Exception("No TargetDataLine found for this device")

        val dataLineInfo = lineInfo as DataLine.Info

        if (!dataLineInfo.isFormatSupported(format)) {
            throw IllegalArgumentException("The specified format is not supported by this device")
        }

        val targetDataLine = AudioSystem.getTargetDataLine(format, mixerInfo)

        targetDataLine.open(format)

        targetDataLine
    }

    override fun capture(device: Device) = callbackFlow {
        val targetDataLine = findMicrophone(device).getOrElse { throwable ->
            close(throwable)
            return@callbackFlow
        }

        try {
            targetDataLine.start()

            val buffer = ByteArray(CapturingService.FRAME_SIZE * 2)

            while (isActive) {
                val bytesRead = targetDataLine.read(buffer, 0, buffer.size)

                if (bytesRead > 0) {
                    send(buffer.copyOfRange(0, bytesRead))
                }
            }
        } finally {
            targetDataLine.stop()
            targetDataLine.close()
        }

        awaitClose { targetDataLine.close() }
    }
}