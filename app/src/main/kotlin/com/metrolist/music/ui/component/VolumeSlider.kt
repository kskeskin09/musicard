/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.utils.rememberEnumPreference

/**
 * Volume as a plain thick bar ("Classic" slider style) with round ends: the set part bright, the rest grey. Tap anywhere
 * on it or slide along it to change the volume.
 */
@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    // "Old" slider style: the plain Material slider, like everywhere else in the app.
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    if (sliderStyle == SliderStyle.DEFAULT) {
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val fill = MaterialTheme.colorScheme.onSurface
    val rest = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .then(
                    if (enabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    onChange((offset.x / size.width).coerceIn(0f, 1f))
                                    onFinished?.invoke()
                                }
                            }.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = { onFinished?.invoke() },
                                    onDragCancel = { onFinished?.invoke() },
                                ) { change, _ ->
                                    change.consume()
                                    onChange((change.position.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                    } else {
                        Modifier
                    },
                ),
    ) {
        val trackHeight = 9.dp.toPx()
        val top = (size.height - trackHeight) / 2f
        val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        drawRoundRect(color = rest, topLeft = Offset(0f, top), size = Size(size.width, trackHeight), cornerRadius = radius)
        val filled = size.width * value.coerceIn(0f, 1f)
        if (filled > 0f) {
            drawRoundRect(
                color = fill,
                topLeft = Offset(0f, top),
                size = Size(filled.coerceAtLeast(trackHeight), trackHeight),
                cornerRadius = radius,
            )
        }
    }
}
