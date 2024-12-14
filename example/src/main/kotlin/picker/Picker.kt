package picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Picker(
    modifier: Modifier,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            Card(elevation = if (index == selectedIndex) 0.dp else 1.dp) {
                Box(
                    modifier = Modifier.weight(1f).clickable { onItemSelected(index) }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}