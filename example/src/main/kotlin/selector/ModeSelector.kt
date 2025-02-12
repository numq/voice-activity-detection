package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.numq.voiceactivitydetection.fvad.FvadVoiceActivityDetectionMode

@Composable
fun ModeSelector(
    modifier: Modifier,
    selectedMode: FvadVoiceActivityDetectionMode,
    selectMode: (FvadVoiceActivityDetectionMode) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = FvadVoiceActivityDetectionMode.entries.map { mode ->
            when (mode) {
                FvadVoiceActivityDetectionMode.QUALITY -> "Quality"

                FvadVoiceActivityDetectionMode.LOW_BITRATE -> "Low bitrate"

                FvadVoiceActivityDetectionMode.AGGRESSIVE -> "Aggressive"

                FvadVoiceActivityDetectionMode.VERY_AGGRESSIVE -> "Very aggressive"
            }
        },
        selectedIndex = FvadVoiceActivityDetectionMode.entries.indexOf(selectedMode),
        selectIndex = { index -> selectMode(FvadVoiceActivityDetectionMode.entries.elementAt(index)) }
    )
}