/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
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
    val powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    return BatteryStatus(percent, charging, powerSave)
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
                            status.copy(
                                systemPowerSave = (ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true,
                            )
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
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
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
