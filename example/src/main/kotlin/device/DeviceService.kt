package device

interface DeviceService {
    suspend fun listCapturingDevices(): Result<List<Device>>
    suspend fun listPlaybackDevices(): Result<List<Device>>

    companion object {
        fun create(): Result<DeviceService> = runCatching { MixerDeviceService() }
    }
}