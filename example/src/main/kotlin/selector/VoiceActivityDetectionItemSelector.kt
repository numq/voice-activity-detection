package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import item.VoiceActivityDetectionItem

@Composable
fun VoiceActivityDetectionItemSelector(
    modifier: Modifier,
    selectedVoiceActivityDetectionItem: VoiceActivityDetectionItem,
    selectVoiceActivityDetectionItem: (VoiceActivityDetectionItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = VoiceActivityDetectionItem.entries.map(VoiceActivityDetectionItem::name),
        selectedIndex = VoiceActivityDetectionItem.entries.indexOf(selectedVoiceActivityDetectionItem),
        selectIndex = { index -> selectVoiceActivityDetectionItem(VoiceActivityDetectionItem.entries.elementAt(index)) }
    )
}