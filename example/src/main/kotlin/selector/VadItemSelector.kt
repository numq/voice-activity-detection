package selector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import interaction.VadItem

@Composable
fun VadItemSelector(
    modifier: Modifier,
    selectedVadItem: VadItem,
    selectVadItem: (VadItem) -> Unit,
) {
    Selector(
        modifier = modifier,
        items = VadItem.entries.map(VadItem::name),
        selectedIndex = VadItem.entries.indexOf(selectedVadItem),
        selectIndex = { index -> selectVadItem(VadItem.entries.elementAt(index)) }
    )
}