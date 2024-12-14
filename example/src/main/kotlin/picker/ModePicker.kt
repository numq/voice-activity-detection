package picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.numq.kvad.KVADMode

@Composable
fun ModePicker(
    modifier: Modifier,
    currentMode: KVADMode,
    changeCurrentMode: (KVADMode) -> Unit,
) {
    Picker(
        modifier = modifier,
        items = KVADMode.entries.map { mode ->
            when (mode) {
                KVADMode.QUALITY -> "Quality"

                KVADMode.LOW_BITRATE -> "Low bitrate"

                KVADMode.AGGRESSIVE -> "Aggressive"

                KVADMode.VERY_AGGRESSIVE -> "Very aggressive"
            }
        },
        selectedIndex = KVADMode.entries.indexOf(currentMode),
        onItemSelected = { index -> changeCurrentMode(KVADMode.entries.elementAt(index)) }
    )
}