package capturing

import device.Device
import kotlinx.coroutines.flow.Flow

interface CapturingService {
    fun capture(device: Device): Flow<ByteArray>

    companion object {
        const val FRAME_SIZE = 160
    }
}