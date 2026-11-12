/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Blurs the bottom of the content behind the tab bar and the mini player. The blur fades in from
 * [startFromBottomPx] (measured up from the bottom edge, the same point where the darkening
 * starts) over a short distance and is full below that. Needs Android 12 for the blur itself;
 * older versions draw the content unchanged.
 */
fun Modifier.bottomEdgeBlur(
    enabled: Boolean,
    startFromBottomPx: () -> Float,
): Modifier =
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        this
    } else {
        composed { bottomEdgeBlurImpl(startFromBottomPx) }
    }

@Composable
private fun Modifier.bottomEdgeBlurImpl(startFromBottomPx: () -> Float): Modifier {
    val sharp = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val masked = rememberGraphicsLayer()
    return drawWithContent {
        sharp.record { this@drawWithContent.drawContent() }
        drawLayer(sharp)
        val region = startFromBottomPx().coerceIn(0f, size.height)
        if (region < 1f) return@drawWithContent
        val top = size.height - region
        val regionSize = IntSize(size.width.toInt(), region.toInt().coerceAtLeast(1))
        val radius = 18.dp.toPx()
        blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
        blurred.record(regionSize) {
            translate(top = -top) { drawLayer(sharp) }
        }
        masked.compositingStrategy = CompositingStrategy.Offscreen
        masked.record(regionSize) {
            drawLayer(blurred)
            // Soft start: the blur grows in over the first part of the region.
            val ramp = (24.dp.toPx() / region).coerceIn(0.01f, 1f)
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        ramp to Color.Black,
                        1f to Color.Black,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }
        translate(top = top) { drawLayer(masked) }
    }
}
