package interaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import capturing.CapturingService
import com.github.numq.vad.VoiceActivityDetection
import device.Device
import device.DeviceService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import playback.PlaybackService
import selector.ModeSelector
import selector.VadItemSelector

@Composable
fun InteractionScreen(
    deviceService: DeviceService,
    fvad: VoiceActivityDetection.Fvad,
    silero: VoiceActivityDetection.Silero,
    capturingService: CapturingService,
    playbackService: PlaybackService,
    handleThrowable: (Throwable) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.Default }

    var deviceJob by remember { mutableStateOf<Job?>(null) }

    var capturingJob by remember { mutableStateOf<Job?>(null) }

    var selectedMode by remember { mutableStateOf(fvad.mode) }

    var selectedVadItem by remember { mutableStateOf(VadItem.FVAD) }

    val capturingDevices = remember { mutableStateListOf<Device>() }

    var selectedCapturingDevice by remember { mutableStateOf<Device?>(null) }

    var refreshRequested by remember { mutableStateOf(true) }

    var isVoiceActivityDetected by remember { mutableStateOf(false) }

    LaunchedEffect(refreshRequested) {
        deviceJob?.cancel()
        deviceJob = null

        if (refreshRequested) {
            deviceJob = coroutineScope.launch {
                deviceService.listCapturingDevices().onSuccess { devices ->
                    if (devices != capturingDevices) {
                        capturingDevices.clear()
                        capturingDevices.addAll(devices)

                        if (selectedCapturingDevice !in capturingDevices) {
                            selectedCapturingDevice = null
                        }
                    }
                }.onFailure(handleThrowable)

                refreshRequested = false
            }
        }
    }

    LaunchedEffect(selectedVadItem, selectedMode, selectedCapturingDevice) {
        isVoiceActivityDetected = false

        capturingJob?.cancelAndJoin()
        capturingJob = null

        if (fvad.mode != selectedMode) {
            fvad.changeMode(selectedMode).onFailure(handleThrowable)
        }

        capturingJob = when (val device = selectedCapturingDevice) {
            null -> return@LaunchedEffect

            else -> coroutineScope.launch {
                val sampleRate = device.sampleRate

                val channels = device.channels

                val chunkSize = when (selectedVadItem) {
                    VadItem.FVAD -> fvad.minimumInputSize(
                        sampleRate = sampleRate,
                        channels = channels
                    )

                    VadItem.SILERO -> silero.minimumInputSize(
                        sampleRate = sampleRate,
                        channels = channels
                    )
                }

                capturingService.capture(
                    device = device,
                    chunkSize = chunkSize
                ).catch {
                    if (it != CancellationException()) {
                        handleThrowable(it)
                    }
                }.collect { pcmBytes ->
                    isVoiceActivityDetected = when (selectedVadItem) {
                        VadItem.FVAD -> fvad

                        VadItem.SILERO -> silero
                    }.detect(
                        pcmBytes = pcmBytes,
                        sampleRate = sampleRate,
                        channels = channels
                    ).onFailure(handleThrowable).getOrDefault(false)

                    if (isVoiceActivityDetected) {
                        playbackService.write(pcmBytes = pcmBytes).getOrThrow()
                    } else {
                        playbackService.play().getOrThrow()

                        when (selectedVadItem) {
                            VadItem.FVAD -> fvad

                            VadItem.SILERO -> silero
                        }.reset()
                    }
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            VadItemSelector(
                modifier = Modifier.fillMaxWidth(),
                selectedVadItem = selectedVadItem
            ) { vad ->
                selectedVadItem = vad

                when (selectedVadItem) {
                    VadItem.FVAD -> fvad

                    VadItem.SILERO -> silero
                }.reset().onFailure(handleThrowable)
            }

            AnimatedVisibility(
                visible = selectedVadItem == VadItem.FVAD,
                enter = expandIn(),
                exit = shrinkOut()
            ) {
                ModeSelector(
                    modifier = Modifier.fillMaxWidth(),
                    selectedMode = selectedMode
                ) { mode ->
                    fvad.changeMode(mode).onSuccess {
                        selectedMode = fvad.mode
                    }.onFailure(handleThrowable)
                }
            }

            if (isVoiceActivityDetected) {
                Text("Voice activity detected", color = Color.Green, modifier = Modifier.padding(8.dp))
            } else {
                Text("Silence", color = Color.Red, modifier = Modifier.padding(8.dp))
            }

            Divider(modifier = Modifier.fillMaxWidth())

            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Capturing devices", modifier = Modifier.padding(8.dp))
                        when (refreshRequested) {
                            true -> IconButton(onClick = {
                                refreshRequested = false
                            }) {
                                Icon(Icons.Default.Cancel, null)
                            }

                            false -> IconButton(onClick = {
                                refreshRequested = true
                            }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    }
                    when {
                        refreshRequested -> Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        else -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(capturingDevices, key = { it.name }) { device ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .alpha(alpha = if (device == selectedCapturingDevice) .5f else 1f).clickable {
                                            selectedCapturingDevice = device.takeIf { it != selectedCapturingDevice }
                                        }) {
                                    Text(device.name, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}