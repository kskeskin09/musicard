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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Blurs the bottom of the content behind the tab bar and the mini player. The blur grows in from
 * [startFromBottomPx] (measured up from the bottom edge, the same point where the darkening
 * starts): light at first, getting stronger over 40 dp, then full. Needs Android 12 for the blur itself;
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

// Steps of the soft start at the top of the blurred area.
private const val BlurSteps = 5

@Composable
private fun Modifier.bottomEdgeBlurImpl(
    enabled: Boolean,
    startFromBottomPx: () -> Float,
): Modifier {
    val sharp = rememberGraphicsLayer()
    // One layer per step of the soft start, plus the full blur below it.
    val steps = List(BlurSteps + 1) { rememberGraphicsLayer() }
    val batterySaverOff = com.metrolist.music.utils.LocalBatterySaver.current.blur
    return drawWithContent {
        val region = startFromBottomPx().coerceIn(0f, size.height)
        if (!enabled || batterySaverOff || blurBroken || region < 1f || size.width < 1f) {
            drawContent()
            return@drawWithContent
        }
        try {
            sharp.record { this@drawWithContent.drawContent() }
            val top = size.height - region
            // The sharp content only above the blurred area. Drawn underneath it as well, it
            // showed through wherever the page itself is see-through (text outside the cards,
            // like section titles), so that text stayed sharp under its blurred copy.
            clipRect(bottom = top) { this@drawWithContent.drawLayer(sharp) }
            val maxRadius = 18.dp.toPx()
            // Soft start: over the first part of the region the blur radius grows step by step
            // (a light blur at the top, the full blur at the bottom of the ramp), instead of a
            // full blur fading in, which looked sharp and then suddenly strong.
            val ramp = 40.dp.toPx().coerceAtMost(region)
            val stepHeight = ramp / BlurSteps
            // Draws [from]..[to] (inside the region) blurred by [radius]. Each layer only holds
            // that band plus a margin, so the blur reads the content around it.
            fun blurBand(layer: androidx.compose.ui.graphics.layer.GraphicsLayer, from: Float, to: Float, radius: Float) {
                if (to - from < 0.5f) return
                val margin = (radius * 2f).coerceAtMost(top + from)
                val layerTop = top + from - margin
                val layerHeight = (to - from + margin + radius * 2f).coerceAtMost(size.height - layerTop)
                layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                layer.record(IntSize(size.width.toInt(), layerHeight.toInt().coerceAtLeast(1))) {
                    translate(top = -layerTop) { drawLayer(sharp) }
                }
                translate(top = layerTop) {
                    clipRect(top = margin, bottom = margin + (to - from)) { drawLayer(layer) }
                }
            }
            for (i in 0 until BlurSteps) {
                val radius = maxRadius * (i + 1f) / (BlurSteps + 1f)
                blurBand(steps[i], i * stepHeight, (i + 1) * stepHeight, radius)
            }
            blurBand(steps[BlurSteps], ramp, region, maxRadius)
        } catch (e: Exception) {
            blurBroken = true
            timber.log.Timber.e(e, "Bottom blur failed; drawing without it")
            drawContent()
        }
    }
}
