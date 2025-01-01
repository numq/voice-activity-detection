package capturing

import device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine

internal class LineCapturingService : CapturingService {
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

        AudioSystem.getTargetDataLine(format, mixerInfo).apply {
            open(format)
        }
    }

    override suspend fun capture(device: Device, chunkSize: Int) = callbackFlow {
        val targetDataLine = findMicrophone(device).getOrElse { throwable ->
            close(throwable)
            return@callbackFlow
        }

        targetDataLine.start()

        while (isActive) {
            val chunk = ByteArray(chunkSize)

            val bytesRead = targetDataLine.read(chunk, 0, chunk.size)

            if (bytesRead > 0) {
                send(chunk.copyOfRange(0, bytesRead))
            }
        }

        targetDataLine.stop()

        awaitClose { targetDataLine.close() }
    }.flowOn(Dispatchers.IO)
}