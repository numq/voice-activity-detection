package capturing

import device.Device
import kotlinx.coroutines.flow.Flow

interface CapturingService {
    fun capture(device: Device, chunkSize: Int): Flow<ByteArray>
}