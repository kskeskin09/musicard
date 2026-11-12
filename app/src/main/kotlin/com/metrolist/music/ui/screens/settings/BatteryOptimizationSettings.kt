/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.StyledSlider
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.BatteryOptReduceBlurKey
import com.metrolist.music.utils.BatteryOptReduceDynamicColorsKey
import com.metrolist.music.utils.BatteryOptReduceLyricsAnimationsKey
import com.metrolist.music.utils.BatteryOptimizationFollowSystemKey
import com.metrolist.music.utils.BatteryOptimizationKey
import com.metrolist.music.utils.BatteryOptimizationThresholdKey
import com.metrolist.music.utils.DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD
import com.metrolist.music.utils.LocalBatterySaver
import com.metrolist.music.utils.rememberBatteryStatus
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

/**
 * Settings > Appearance > Battery optimization: below a battery level the user picks, heavy
 * graphics are turned down. Each effect can be kept out of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationSettings(navController: NavController) {
    val (enabled, onEnabledChange) = rememberPreference(BatteryOptimizationKey, false)
    val (threshold, onThresholdChange) =
        rememberPreference(BatteryOptimizationThresholdKey, DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD)
    val (followSystem, onFollowSystemChange) = rememberPreference(BatteryOptimizationFollowSystemKey, true)
    val battery = rememberBatteryStatus()
    val saver = LocalBatterySaver.current
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp),
    ) {
        // Status card: battery now and whether optimization is turning things down.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (saver.active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    ).padding(20.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.battery_saver),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text =
                        when {
                            !enabled -> stringResource(R.string.battery_optimization_status_off)
                            saver.active -> stringResource(R.string.battery_optimization_status_active, battery.level)
                            battery.charging -> stringResource(R.string.battery_optimization_status_charging, battery.level)
                            else -> stringResource(R.string.battery_optimization_status_idle, battery.level)
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.battery_optimization_status_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            items =
                listOf(
                    switchItem(
                        icon = painterResource(R.drawable.battery_saver),
                        title = R.string.battery_optimization,
                        description = R.string.battery_optimization_enable_desc,
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.speed),
                        enabled = enabled,
                        title = { Text(stringResource(R.string.battery_optimization_threshold, sliderValue.roundToInt())) },
                        description = {
                            Column {
                                Text(stringResource(R.string.battery_optimization_threshold_desc))
                                StyledSlider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = { onThresholdChange(sliderValue.roundToInt()) },
                                    valueRange = 5f..80f,
                                    steps = 14,
                                    enabled = enabled,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.contrast),
                        title = R.string.battery_optimization_follow_system,
                        description = R.string.battery_optimization_follow_system_desc,
                        checked = followSystem,
                        onCheckedChange = onFollowSystemChange,
                        enabled = enabled,
                    ),
                ),
        )

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.battery_optimization_turn_off),
            items =
                listOf(
                    effectItem(BatteryOptReduceBlurKey, R.drawable.blur_on, R.string.battery_opt_blur, R.string.battery_opt_blur_desc, enabled),
                    effectItem(
                        BatteryOptReduceDynamicColorsKey,
                        R.drawable.palette,
                        R.string.battery_opt_dynamic_colors,
                        R.string.battery_opt_dynamic_colors_desc,
                        enabled,
                    ),
                    effectItem(
                        BatteryOptReduceLyricsAnimationsKey,
                        R.drawable.lyrics,
                        R.string.battery_opt_lyrics_animations,
                        R.string.battery_opt_lyrics_animations_desc,
                        enabled,
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.battery_optimization)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun effectItem(
    key: Preferences.Key<Boolean>,
    iconRes: Int,
    title: Int,
    description: Int,
    enabled: Boolean,
): Material3SettingsItem {
    val (checked, onCheckedChange) = rememberPreference(key, true)
    return switchItem(
        icon = painterResource(iconRes),
        title = title,
        description = description,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    )
}

private fun switchItem(
    icon: Painter,
    title: Int,
    description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) = Material3SettingsItem(
    icon = icon,
    enabled = enabled,
    title = { Text(stringResource(title)) },
    description = { Text(stringResource(description)) },
    trailingContent = {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            thumbContent = {
                Icon(
                    painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    },
    onClick = { onCheckedChange(!checked) },
)
