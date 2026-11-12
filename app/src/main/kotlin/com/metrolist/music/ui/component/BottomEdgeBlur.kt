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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        this
    } else {
        // Always the same modifier, so switching the setting never drops layers while they are
        // still being drawn; when off it only draws the content.
        composed { bottomEdgeBlurImpl(enabled, startFromBottomPx) }
    }

// Set once drawing the blur fails on a device, so the app keeps working without it.
@Volatile
private var blurBroken = false

@Composable
private fun Modifier.bottomEdgeBlurImpl(
    enabled: Boolean,
    startFromBottomPx: () -> Float,
): Modifier {
    val sharp = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    return drawWithContent {
        val region = startFromBottomPx().coerceIn(0f, size.height)
        if (!enabled || blurBroken || region < 1f || size.width < 1f) {
            drawContent()
            return@drawWithContent
        }
        try {
            sharp.record { this@drawWithContent.drawContent() }
            drawLayer(sharp)
            val top = size.height - region
            val regionSize = IntSize(size.width.toInt(), region.toInt().coerceAtLeast(1))
            val radius = 18.dp.toPx()
            blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            blurred.record(regionSize) {
                translate(top = -top) { drawLayer(sharp) }
            }
            // Soft start: the blur grows in over the first 24 dp in thin bands, then is full.
            val ramp = 24.dp.toPx().coerceAtMost(region)
            val bands = 8
            val bandHeight = ramp / bands
            translate(top = top) {
                for (i in 0 until bands) {
                    val bandTop = i * bandHeight
                    val bandBottom = (i + 1) * bandHeight
                    clipRect(top = bandTop, bottom = bandBottom) {
                        // The layer's own alpha is read when the frame is drawn, not here, so each
                        // band gets its strength from a save layer instead.
                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(
                                Rect(0f, bandTop, size.width, bandBottom),
                                Paint().apply { alpha = (i + 1f) / (bands + 1f) },
                            )
                            drawLayer(blurred)
                            canvas.restore()
                        }
                    }
                }
                clipRect(top = ramp, bottom = region) { drawLayer(blurred) }
            }
        } catch (e: Exception) {
            blurBroken = true
            timber.log.Timber.e(e, "Bottom blur failed; drawing without it")
            drawContent()
        }
    }
}
