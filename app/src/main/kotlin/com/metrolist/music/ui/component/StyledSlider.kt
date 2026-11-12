/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.utils.rememberEnumPreference

/**
 * A slider that follows the "Player slider style" setting, so every slider in the app (except the
 * mini player's progress bar) looks the same: Classic is a thick bar without a thumb, Old is the
 * plain Material slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    when (sliderStyle) {
        SliderStyle.SLIM -> {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                colors = colors,
                thumb = { Spacer(Modifier.size(0.dp)) },
                track = { state ->
                    PlayerSliderTrack(
                        sliderState = state,
                        colors = colors,
                        showTicks = false,
                    )
                },
            )
        }

        SliderStyle.DEFAULT -> {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                colors = colors,
            )
        }
    }
}
