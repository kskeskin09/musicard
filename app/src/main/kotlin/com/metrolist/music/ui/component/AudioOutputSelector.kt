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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** The output the user picked in the app (null: let the system decide). */
object AudioOutputChoice {
    val selectedKey = MutableStateFlow<String?>(null)

    private val guardScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private var guardJob: kotlinx.coroutines.Job? = null

    /** True for the moment the output is being changed; the app keeps showing "playing" then. */
    val switching = MutableStateFlow(false)

    /**
     * Moves playback to [device] (null: the system decides).
     *
     * When the route changes while audio is flowing, phones throw away the audio already handed
     * to the old output, so the song jumped back, the lyrics stopped, and then it all caught up
     * again. Instead the player is held quietly while the output changes and then carries on from
     * exactly where it was. The app (play button, lyrics, progress) just waits for that moment
     * and doesn't show a pause.
     */
    fun switchTo(
        player: androidx.media3.exoplayer.ExoPlayer,
        device: AudioDeviceInfo?,
    ) {
        guardJob?.cancel()
        val wasPlaying = player.playWhenReady
        if (!wasPlaying) {
            player.setPreferredAudioDevice(device)
            return
        }
        val startPosition = player.currentPosition
        val startIndex = player.currentMediaItemIndex
        switching.value = true
        player.playWhenReady = false
        player.setPreferredAudioDevice(device)
        guardJob =
            guardScope.launch {
                try {
                    // Long enough for the new output to be ready, short enough not to be noticed.
                    kotlinx.coroutines.delay(SwitchHoldMillis)
                    if (player.currentMediaItemIndex == startIndex) player.seekTo(startPosition)
                    player.playWhenReady = true
                } finally {
                    switching.value = false
                }
            }
    }

    private const val SwitchHoldMillis = 350L
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
                        runCatching { AudioOutputChoice.switchTo(playerConnection.service.player, null) }
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
            add(OutputOption(null, stringResource(R.string.audio_output_auto), R.drawable.audio_output_auto, null))
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.let {
                add(OutputOption(outputKey(it), phoneLabel, R.drawable.audio_output_phone, it))
            }
            devices
                .filter { it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                .forEach {
                    val icon =
                        when (it.type) {
                            27 -> R.drawable.audio_output_bluetooth
                            else -> R.drawable.audio_output_headphones
                        }
                    add(OutputOption(outputKey(it), it.productName?.toString()?.takeIf { n -> n.isNotBlank() } ?: "Audio", icon, it))
                }
        }

    val onPick: (OutputOption) -> Unit = { option ->
        // Tapping the output already in use does nothing (no needless rerouting).
        if (option.key != AudioOutputChoice.selectedKey.value) {
            AudioOutputChoice.selectedKey.value = option.key
            runCatching { AudioOutputChoice.switchTo(playerConnection.service.player, option.device) }
        }
    }

    // At most three choices: System auto, the phone and one connected output (the one picked, or
    // else Bluetooth before wired).
    val shown =
        buildList {
            addAll(options.filter { it.key == null || it.device?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER })
            val external = options.filter { it.key != null && it.device?.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            (external.firstOrNull { it.key == selectedKey } ?: external.minByOrNull { outputRank(it.device) })?.let { add(it) }
        }
    val selectedIndex = shown.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        OutputBar(shown, selectedIndex, onPick)
        ActiveOutputLine(
            devices = devices,
            selected = shown.getOrNull(selectedIndex),
            phoneLabel = phoneLabel,
        )
    }
}

/** Lower is preferred: Bluetooth, then wired and USB, then anything else. */
private fun outputRank(device: AudioDeviceInfo?): Int =
    when (device?.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 26, 27 -> 0
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 1
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> 2
        else -> 3
    }

private val OutputSize = 66.dp

/**
 * The choices inside one rounded bar (like the Library filters). The first sits at the left end,
 * the last at the right end, a third in the middle; a white circle marks the one in use and slides
 * over to a new choice.
 */
@Composable
private fun OutputBar(
    options: List<OutputOption>,
    selectedIndex: Int,
    onPick: (OutputOption) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(OutputSize)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val count = options.size
        val step = if (count > 1) (maxWidth - OutputSize) / (count - 1) else 0.dp
        val indicatorX by androidx.compose.animation.core.animateDpAsState(
            targetValue = step * selectedIndex,
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 700f),
            label = "outputIndicator",
        )
        // The white circle under the chosen symbol.
        androidx.compose.foundation.layout.Box(
            Modifier
                .offset(x = indicatorX)
                .size(OutputSize)
                .clip(CircleShape)
                .background(onSurface),
        )
        options.forEachIndexed { index, option ->
            val chosen = index == selectedIndex
            val tint by androidx.compose.animation.animateColorAsState(
                targetValue = if (chosen) MaterialTheme.colorScheme.surface else onSurface,
                label = "outputTint",
            )
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .offset(x = step * index)
                        .size(OutputSize)
                        .clip(CircleShape)
                        .clickable { onPick(option) }
                        .semantics { contentDescription = option.label },
            ) {
                Icon(
                    painter = painterResource(option.icon),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** The device the sound is going to now, when the system decides. */
private fun activeOutput(
    context: Context,
    devices: List<AudioDeviceInfo>,
): AudioDeviceInfo? {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attributes =
            android.media.AudioAttributes
                .Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        runCatching { audioManager.getAudioDevicesForAttributes(attributes) }
            .getOrNull()
            ?.firstOrNull()
            ?.let { routed -> return devices.firstOrNull { it.id == routed.id } ?: routed }
    }
    // Older Android: the system plays on the newest kind of output connected.
    return devices.filter { it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }.minByOrNull { outputRank(it) }
        ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}

private fun isBluetooth(device: AudioDeviceInfo) =
    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || device.type == 26 || device.type == 27

private fun hasBluetoothPermission(context: Context) =
    android.os.Build.VERSION.SDK_INT < 31 ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

/**
 * Battery of a Bluetooth output, as Android reports it (one number for the whole device; the
 * left / right earbud levels are not given to apps). Null when unknown.
 */
@android.annotation.SuppressLint("MissingPermission", "DiscouragedPrivateApi")
private fun bluetoothBattery(
    context: Context,
    device: AudioDeviceInfo,
): Int? {
    if (android.os.Build.VERSION.SDK_INT < 28 || !hasBluetoothPermission(context)) return null
    val address = device.address.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val remote = manager.adapter?.getRemoteDevice(address) ?: return null
        val level = remote.javaClass.getMethod("getBatteryLevel").invoke(remote) as Int
        level.takeIf { it in 0..100 }
    }.getOrNull()
}

private fun phoneBattery(context: Context): Int? {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
}

/** Under the bar: the name of the device playing now and its battery. */
@Composable
private fun ActiveOutputLine(
    devices: List<AudioDeviceInfo>,
    selected: OutputOption?,
    phoneLabel: String,
) {
    val context = LocalContext.current
    // Battery news from Bluetooth devices, and a slow refresh for the phone's own battery.
    var tick by remember { mutableStateOf(0) }
    DisposableEffect(context) {
        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    c: Context?,
                    intent: android.content.Intent?,
                ) {
                    tick++
                }
            }
        val filter =
            android.content.IntentFilter().apply {
                addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
                addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) { tick++ }

    val device = selected?.device ?: remember(devices, tick) { activeOutput(context, devices) }
    device ?: return
    val name =
        if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
            phoneLabel
        } else {
            device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audio"
        }
    val battery =
        remember(device, tick) {
            when {
                device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> phoneBattery(context)
                isBluetooth(device) -> bluetoothBattery(context, device)
                else -> null
            }
        }
    val needsPermission = isBluetooth(device) && !hasBluetoothPermission(context) && !permissionAsked
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Text(
            text = if (battery != null) "$name · $battery%" else name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (needsPermission && android.os.Build.VERSION.SDK_INT >= 31) {
            Text(
                text = " · " + stringResource(R.string.audio_output_show_battery),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.clickable {
                        permissionAsked = true
                        permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                    },
            )
        }
    }
}
