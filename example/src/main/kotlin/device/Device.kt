package device

data class Device(
    val name: String,
    val sampleRate: Int,
    val sampleSizeInBits: Int,
    val channels: Int,
    val isSigned: Boolean,
    val isBigEndian: Boolean,
)