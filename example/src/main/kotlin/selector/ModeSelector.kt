package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.numq.vad.fvad.VoiceActivityDetectionMode

@Composable
fun ModeSelector(
    modifier: Modifier,
    selectedMode: VoiceActivityDetectionMode,
    selectMode: (VoiceActivityDetectionMode) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = VoiceActivityDetectionMode.entries.map { mode ->
            when (mode) {
                VoiceActivityDetectionMode.QUALITY -> "Quality"

                VoiceActivityDetectionMode.LOW_BITRATE -> "Low bitrate"

                VoiceActivityDetectionMode.AGGRESSIVE -> "Aggressive"

                VoiceActivityDetectionMode.VERY_AGGRESSIVE -> "Very aggressive"
            }
        },
        selectedIndex = VoiceActivityDetectionMode.entries.indexOf(selectedMode),
        selectIndex = { index -> selectMode(VoiceActivityDetectionMode.entries.elementAt(index)) }
    )
}