package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import interaction.VoiceActivityDetectionItem

@Composable
fun VadItemSelector(
    modifier: Modifier,
    selectedVadItem: VoiceActivityDetectionItem,
    selectVadItem: (VoiceActivityDetectionItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = VoiceActivityDetectionItem.entries.map(VoiceActivityDetectionItem::visibleName),
        selectedIndex = VoiceActivityDetectionItem.entries.indexOf(selectedVadItem),
        selectIndex = { index -> selectVadItem(VoiceActivityDetectionItem.entries.elementAt(index)) }
    )
}