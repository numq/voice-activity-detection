package device

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

internal class MixerDeviceService : DeviceService {
    private fun createDevice(name: String, format: AudioFormat) = with(format) {
        Device(
            name = name,
            sampleRate = sampleRate.toInt(),
            sampleSizeInBits = sampleSizeInBits,
            channels = channels,
            isSigned = encoding == AudioFormat.Encoding.PCM_SIGNED,
            isBigEndian = isBigEndian
        )
    }

    override suspend fun listCapturingDevices() = runCatching {
        AudioSystem.getMixerInfo().flatMap { mixerInfo ->
            AudioSystem.getMixer(mixerInfo).let { mixer ->
                mixer.targetLineInfo.mapNotNull { lineInfo ->
                    (mixer.getLine(lineInfo) as? TargetDataLine)?.format?.let { format ->
                        createDevice(name = mixerInfo.name, format = format)
                    }
                }
            }
        }
    }

    override suspend fun listPlaybackDevices() = runCatching {
        AudioSystem.getMixerInfo().flatMap { mixerInfo ->
            AudioSystem.getMixer(mixerInfo).let { mixer ->
                mixer.sourceLineInfo.mapNotNull { lineInfo ->
                    (mixer.getLine(lineInfo) as? SourceDataLine)?.format?.let { format ->
                        createDevice(name = mixerInfo.name, format = format)
                    }
                }
            }
        }
    }
}