/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

// Settings > Appearance > Battery optimization
val BatteryOptimizationKey = booleanPreferencesKey("batteryOptimization")
val BatteryOptimizationThresholdKey = intPreferencesKey("batteryOptimizationThreshold")
val BatteryOptimizationFollowSystemKey = booleanPreferencesKey("batteryOptimizationFollowSystem")
val BatteryOptReduceBlurKey = booleanPreferencesKey("batteryOptReduceBlur")
val BatteryOptReduceDynamicColorsKey = booleanPreferencesKey("batteryOptReduceDynamicColors")
val BatteryOptReduceLyricsAnimationsKey = booleanPreferencesKey("batteryOptReduceLyricsAnimations")
val BatteryOptReduceSongChangeAnimationKey = booleanPreferencesKey("batteryOptReduceSongChangeAnimation")

const val DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD = 20

/** Battery level (0-100), whether the phone is charging, and whether system battery saver is on. */
@Immutable
data class BatteryStatus(
    val level: Int = 100,
    val charging: Boolean = false,
    val systemPowerSave: Boolean = false,
)

/**
 * What battery optimization is turning off right now. Each flag is true only while optimization is
 * active and the user left that effect in the "turn off" list.
 */
@Immutable
data class BatterySaverState(
    val active: Boolean = false,
    val blur: Boolean = false,
    val dynamicColors: Boolean = false,
    val lyricsAnimations: Boolean = false,
    val songChangeAnimation: Boolean = false,
)

val LocalBatterySaver = staticCompositionLocalOf { BatterySaverState() }

private fun Intent?.toBatteryStatus(context: Context): BatteryStatus {
    val level = this?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = this?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else 100
    val status = this?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryStatus(percent, charging, isSystemPowerSave(context))
}

// Where phones keep their battery saver switch. Android's own one is in PowerManager and in
// Settings.Global "low_power"; Xiaomi (MIUI / HyperOS) keeps its own battery saver and ultra
// battery saver in Settings.System and does not always turn Android's one on.
private val PowerSaveGlobalKeys = listOf("low_power")
private val PowerSaveSystemKeys = listOf("POWER_SAVE_MODE_OPEN", "power_supersave_mode_open")

private fun settingOn(read: () -> Int): Boolean = runCatching { read() == 1 }.getOrDefault(false)

/** True when the phone's own battery saver is on, also on phones that keep it in their own setting. */
fun isSystemPowerSave(context: Context): Boolean {
    if ((context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true) return true
    val resolver = context.contentResolver
    if (PowerSaveGlobalKeys.any { key -> settingOn { Settings.Global.getInt(resolver, key, 0) } }) return true
    return PowerSaveSystemKeys.any { key -> settingOn { Settings.System.getInt(resolver, key, 0) } }
}

/** Follows the battery level and charging state while the composition is alive. */
@Composable
fun rememberBatteryStatus(): BatteryStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(BatteryStatus()) }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    status =
                        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                            intent.toBatteryStatus(ctx)
                        } else {
                            status.copy(systemPowerSave = isSystemPowerSave(ctx))
                        }
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        // ACTION_BATTERY_CHANGED is sticky, so the current value comes back right away.
        val sticky =
            runCatching {
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            }.getOrNull()
        status = sticky.toBatteryStatus(context)

        // Some phones (Xiaomi) don't send the power save broadcast for their own battery saver,
        // so also watch the settings it is stored in.
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    status = status.copy(systemPowerSave = isSystemPowerSave(context))
                }
            }
        val resolver = context.contentResolver
        val uris =
            PowerSaveGlobalKeys.map { Settings.Global.getUriFor(it) } +
                PowerSaveSystemKeys.map { Settings.System.getUriFor(it) }
        uris.forEach { uri -> runCatching { resolver.registerContentObserver(uri, false, observer) } }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }
    // Check again whenever the app comes back to the front, in case a change was missed.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    status = status.copy(systemPowerSave = isSystemPowerSave(context))
                }
            }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }
    return status
}

/** Works out which effects battery optimization turns off at the moment. */
@Composable
fun rememberBatterySaverState(): BatterySaverState {
    val enabled by rememberPreference(BatteryOptimizationKey, false)
    val threshold by rememberPreference(BatteryOptimizationThresholdKey, DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD)
    val followSystem by rememberPreference(BatteryOptimizationFollowSystemKey, true)
    val reduceBlur by rememberPreference(BatteryOptReduceBlurKey, true)
    val reduceDynamicColors by rememberPreference(BatteryOptReduceDynamicColorsKey, true)
    val reduceLyricsAnimations by rememberPreference(BatteryOptReduceLyricsAnimationsKey, true)
    val reduceSongChangeAnimation by rememberPreference(BatteryOptReduceSongChangeAnimationKey, true)

    if (!enabled) return BatterySaverState()
    val battery = rememberBatteryStatus()
    val active =
        !battery.charging && (battery.level <= threshold || (followSystem && battery.systemPowerSave))
    if (!active) return BatterySaverState()
    return BatterySaverState(
        active = true,
        blur = reduceBlur,
        dynamicColors = reduceDynamicColors,
        lyricsAnimations = reduceLyricsAnimations,
        songChangeAnimation = reduceSongChangeAnimation,
    )
}

/**
 * Whether battery optimization is on and active right now, for code outside Compose (for example
 * the music service). Reads the sticky battery broadcast, so no receiver stays registered.
 */
fun isBatteryOptimizationActive(
    context: Context,
    enabled: Boolean,
    threshold: Int,
    followSystem: Boolean,
): Boolean {
    if (!enabled) return false
    val status =
        runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull().toBatteryStatus(context)
    return !status.charging && (status.level <= threshold || (followSystem && status.systemPowerSave))
}
