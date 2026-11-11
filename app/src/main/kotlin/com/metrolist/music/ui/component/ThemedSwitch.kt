/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The app's switch (imported as `Switch` across the app). On: a bright track with a dark knob.
 * Off: the track stays visible in a paler white with a black knob, and no mark on it.
 */
@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") colors: SwitchColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val dark = MaterialTheme.colorScheme.surface
    val offTrack = onSurface.copy(alpha = 0.38f)
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = if (checked) thumbContent else null,
        enabled = enabled,
        interactionSource = interactionSource,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = dark,
                checkedTrackColor = onSurface,
                checkedBorderColor = Color.Transparent,
                checkedIconColor = onSurface,
                uncheckedThumbColor = Color.Black,
                uncheckedTrackColor = offTrack,
                uncheckedBorderColor = Color.Transparent,
                uncheckedIconColor = offTrack,
                disabledCheckedThumbColor = dark.copy(alpha = 0.6f),
                disabledCheckedTrackColor = onSurface.copy(alpha = 0.3f),
                disabledCheckedBorderColor = Color.Transparent,
                disabledUncheckedThumbColor = Color.Black.copy(alpha = 0.6f),
                disabledUncheckedTrackColor = onSurface.copy(alpha = 0.15f),
                disabledUncheckedBorderColor = Color.Transparent,
            ),
    )
}
