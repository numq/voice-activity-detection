package capturing

import device.Device
import kotlinx.coroutines.flow.Flow

interface CapturingService {
    suspend fun capture(device: Device, chunkSize: Int): Flow<ByteArray>

    companion object {
        fun create(): Result<CapturingService> = runCatching { LineCapturingService() }
    }
}