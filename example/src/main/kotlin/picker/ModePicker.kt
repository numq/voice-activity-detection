package picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.numq.vad.VoiceActivityDetectionMode

@Composable
fun ModePicker(
    modifier: Modifier,
    currentMode: VoiceActivityDetectionMode,
    changeCurrentMode: (VoiceActivityDetectionMode) -> Unit,
) {
    Picker(
        modifier = modifier,
        items = VoiceActivityDetectionMode.entries.map { mode ->
            when (mode) {
                VoiceActivityDetectionMode.QUALITY -> "Quality"

                VoiceActivityDetectionMode.LOW_BITRATE -> "Low bitrate"

                VoiceActivityDetectionMode.AGGRESSIVE -> "Aggressive"

                VoiceActivityDetectionMode.VERY_AGGRESSIVE -> "Very aggressive"
            }
        },
        selectedIndex = VoiceActivityDetectionMode.entries.indexOf(currentMode),
        onItemSelected = { index -> changeCurrentMode(VoiceActivityDetectionMode.entries.elementAt(index)) }
    )
}