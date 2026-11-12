/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import com.metrolist.music.lyrics.LyricsEntry

sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(
        val afterLineIndex: Int,
        val gapMs: Long,
        val gapStartMs: Long,
        val gapEndMs: Long,
        val nextAgent: String?
    ) : LyricsListItem()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val targetHeightDp = 72.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.CenterStart
    ) {
        InterludeDots(
            progress = animatedProgress,
            positionMs = currentPositionMs,
            color = color,
            dotSize = 14.dp,
        )
    }
}

/**
 * Three dots shown during an instrumental break. Each dot lights up in turn as the break
 * goes on (one third each), and the group breathes gently. Everything is derived from the
 * playback position, so no extra animation loop runs while the lyrics are on screen.
 */
@Composable
fun InterludeDots(
    progress: Float,
    positionMs: Long,
    color: Color,
    dotSize: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = dotSize * 0.75f
    Canvas(
        modifier = modifier
            .width(dotSize * 3 + spacing * 2 + dotSize * 0.4f)
            .height(dotSize * 1.4f)
    ) {
        val dotPx = dotSize.toPx()
        val gapPx = spacing.toPx()
        // Slow breathing (about 2.4 s per cycle); swells a little more just before the break ends.
        val breath = sin(positionMs / 2400.0 * 2 * PI).toFloat()
        val endSwell = ((progress - 0.9f) / 0.1f).coerceIn(0f, 1f)
        val scale = 1f + 0.06f * breath + 0.12f * endSwell
        val radius = dotPx / 2f * scale
        val cy = size.height / 2f
        val startX = (size.width - (dotPx * 3 + gapPx * 2)) / 2f + dotPx / 2f
        for (i in 0 until 3) {
            val fill = (progress * 3f - i).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = color.alpha * (0.3f + 0.7f * fill)),
                radius = radius,
                center = Offset(startX + i * (dotPx + gapPx), cy),
            )
        }
    }
}
