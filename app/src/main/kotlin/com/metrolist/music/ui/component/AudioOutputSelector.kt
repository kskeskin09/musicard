/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

/** The output the user picked in the app (null: let the system decide). */
object AudioOutputChoice {
    val selectedKey = MutableStateFlow<String?>(null)
}

private data class OutputOption(
    val key: String?,
    val label: String,
    val icon: Int,
    val device: AudioDeviceInfo?,
)

private val ExternalOutputTypes =
    setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        26, // TYPE_BLE_HEADSET
        27, // TYPE_BLE_SPEAKER
    )

private fun outputKey(device: AudioDeviceInfo) = "${device.type}:${device.productName}"

private fun currentOutputs(context: Context): List<AudioDeviceInfo> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager
        .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.isSink && (it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || it.type in ExternalOutputTypes) }
        .distinctBy { outputKey(it) }
}

/**
 * Where the sound goes: System auto, the phone's speaker and every connected output. Two or three
 * choices spread across the full width (first on the left, last on the right); with more, the row
 * scrolls sideways.
 */
@Composable
fun AudioOutputSelector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val (username) = rememberPreference(ListenTogetherUsernameKey, "")
    var devices by remember { mutableStateOf(currentOutputs(context)) }
    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val callback =
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    devices = currentOutputs(context)
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    devices = currentOutputs(context)
                    val selected = AudioOutputChoice.selectedKey.value
                    if (selected != null && devices.none { outputKey(it) == selected }) {
                        AudioOutputChoice.selectedKey.value = null
                        runCatching { playerConnection.service.player.setPreferredAudioDevice(null) }
                    }
                }
            }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }
    val selectedKey by AudioOutputChoice.selectedKey.collectAsState()

    val phoneLabel =
        if (username.isNotBlank()) {
            stringResource(R.string.audio_output_phone, username.trim())
        } else {
            stringResource(R.string.audio_output_phone_default)
        }
    val options =
        buildList {
            add(OutputOption(null, stringResource(R.string.audio_output_auto), R.drawable.volume_up, null))
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.let {
                add(OutputOption(outputKey(it), phoneLabel, R.drawable.smartphone, it))
            }
            devices
                .filter { it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                .forEach {
                    val icon =
                        when (it.type) {
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 26, 27 -> R.drawable.bluetooth
                            else -> R.drawable.headphones
                        }
                    add(OutputOption(outputKey(it), it.productName?.toString()?.takeIf { n -> n.isNotBlank() } ?: "Audio", icon, it))
                }
        }

    val onPick: (OutputOption) -> Unit = { option ->
        AudioOutputChoice.selectedKey.value = option.key
        runCatching { playerConnection.service.player.setPreferredAudioDevice(option.device) }
    }

    if (options.size <= 3) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                OutputChip(option, option.key == selectedKey, onPick, Modifier.widthIn(max = 150.dp))
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            options.forEach { option ->
                OutputChip(option, option.key == selectedKey, onPick, Modifier.widthIn(max = 180.dp))
            }
        }
    }
}

@Composable
private fun OutputChip(
    option: OutputOption,
    selected: Boolean,
    onPick: (OutputOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = { onPick(option) },
        shape = CircleShape,
        color = if (selected) onSurface else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else onSurface,
        modifier = modifier.height(44.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
