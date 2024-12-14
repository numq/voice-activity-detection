package picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.numq.kvad.KVADSampleRate

@Composable
fun SampleRatePicker(
    modifier: Modifier,
    currentSampleRate: KVADSampleRate,
    changeCurrentSampleRate: (KVADSampleRate) -> Unit,
) {
    Picker(
        modifier = modifier,
        items = KVADSampleRate.entries.map { sampleRate -> "${sampleRate.value}" },
        selectedIndex = KVADSampleRate.entries.indexOf(currentSampleRate),
        onItemSelected = { index -> changeCurrentSampleRate(KVADSampleRate.entries.elementAt(index)) }
    )
}