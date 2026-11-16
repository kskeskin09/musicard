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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
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

// Levels of the soft start at the top of the blurred area.
private const val BlurSteps = 6

@Composable
private fun Modifier.bottomEdgeBlurImpl(
    enabled: Boolean,
    startFromBottomPx: () -> Float,
): Modifier {
    val sharp = rememberGraphicsLayer()
    // One layer per blurred level of the soft start (the last one is the full blur).
    val steps = List(BlurSteps) { rememberGraphicsLayer() }
    val batterySaverOff = com.metrolist.music.utils.LocalBatterySaver.current.blur
    return drawWithContent {
        val region = startFromBottomPx().coerceIn(0f, size.height)
        if (!enabled || batterySaverOff || blurBroken || region < 1f || size.width < 1f) {
            drawContent()
            return@drawWithContent
        }
        try {
            sharp.record { this@drawWithContent.drawContent() }
            // Whole pixels: a band that starts between two pixels lands on a different pixel
            // row every frame while the page scrolls, which made the bottom shimmer.
            val top = kotlin.math.floor(size.height - region)
            val height = size.height - top
            val width = size.width
            // The sharp content only above the blurred area. Drawn underneath it as well, it
            // showed through wherever the page itself is see-through (text outside the cards,
            // like section titles), so that text stayed sharp under its blurred copy.
            clipRect(bottom = top) { this@drawWithContent.drawLayer(sharp) }
            val maxRadius = 18.dp.toPx()
            // Soft start: over the first part of the region the blur grows from none to full.
            // Level k (k = 0 sharp .. BlurSteps full) is strongest at k * stepHeight and fades
            // into its neighbours over one step on each side. The fades always add up to one and
            // are added together (Plus), so the blur grows smoothly instead of in visible bands.
            val ramp = 40.dp.toPx().coerceAtMost(height)
            val stepHeight = ramp / BlurSteps
            val canvas = drawContext.canvas
            val plus = Paint().apply { blendMode = BlendMode.Plus }
            canvas.saveLayer(Rect(0f, top, width, size.height), Paint())
            for (k in 0..BlurSteps) {
                val peak = k * stepHeight
                val from = if (k == 0) 0f else peak - stepHeight
                val to = if (k == BlurSteps) height else peak + stepHeight
                if (to - from < 0.5f) continue
                canvas.saveLayer(Rect(0f, top + from, width, top + to), plus)
                if (k == 0) {
                    drawLayer(sharp)
                } else {
                    val radius = maxRadius * k / BlurSteps
                    // The layer holds the band plus a margin, so the blur reads the content
                    // around it; mirrored at the screen's edge instead of stretching its last
                    // pixel row, which flickered as rows scrolled past.
                    val margin = (radius * 2f).coerceAtMost(top + from)
                    val layerTop = kotlin.math.floor(top + from - margin)
                    val layerHeight = kotlin.math.ceil((top + to + radius * 2f).coerceAtMost(size.height) - layerTop)
                    val layer = steps[k - 1]
                    layer.renderEffect = BlurEffect(radius, radius, TileMode.Mirror)
                    layer.record(IntSize(width.toInt(), layerHeight.toInt().coerceAtLeast(1))) {
                        translate(top = -layerTop) { drawLayer(sharp) }
                    }
                    translate(top = layerTop) { drawLayer(layer) }
                }
                val peakAt = ((peak - from) / (to - from)).coerceIn(0f, 1f)
                val stops =
                    when (k) {
                        0 -> arrayOf(0f to Color.Black, 1f to Color.Transparent)
                        BlurSteps -> arrayOf(0f to Color.Transparent, peakAt to Color.Black, 1f to Color.Black)
                        else -> arrayOf(0f to Color.Transparent, peakAt to Color.Black, 1f to Color.Transparent)
                    }
                drawRect(
                    brush = Brush.verticalGradient(*stops, startY = top + from, endY = top + to),
                    topLeft = Offset(0f, top + from),
                    size = Size(width, to - from),
                    blendMode = BlendMode.DstIn,
                )
                canvas.restore()
            }
            canvas.restore()
        } catch (e: Exception) {
            blurBroken = true
            timber.log.Timber.e(e, "Bottom blur failed; drawing without it")
            drawContent()
        }
    }
}
