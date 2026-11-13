/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.constants.PageEdgeToEdgeCoverKey
import com.metrolist.music.utils.rememberPreference

/** Whether album and playlist covers fill the top of their page up to the screen edges. */
@Composable
fun rememberPageEdgeToEdgeCover(): Boolean {
    val edgeToEdge by rememberPreference(PageEdgeToEdgeCoverKey, defaultValue = false)
    return edgeToEdge
}

/**
 * The frame of the big cover at the top of an album or playlist page.
 *
 * Normally a rounded card of [size] with a shadow. With "Edge-to-edge cover" on, the cover is as
 * wide as the screen, starts at the very top (under the status bar and the top bar, [pullUp] being
 * the space the header leaves above the cover) and fades out at the bottom into the page
 * background, whether that is a color or the glass background.
 */
@Composable
fun PageCoverFrame(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    corner: Dp = 16.dp,
    shadow: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    pullUp: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!rememberPageEdgeToEdgeCover()) {
        val shape = RoundedCornerShape(corner)
        Surface(
            modifier =
                modifier
                    .size(size)
                    .then(if (shadow > 0.dp) Modifier.shadow(elevation = shadow, shape = shape) else Modifier),
            shape = shape,
            color = color,
        ) {
            Box(Modifier.fillMaxSize(), content = content)
        }
        return
    }
    val pull = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding() + pullUp
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // A light veil behind the top bar so its buttons stay visible on any cover.
    val veil = if (dark) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // Drawn from the top of the screen; the header below starts where it did before.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val pullPx = pull.roundToPx()
                    layout(placeable.width, (placeable.height - pullPx).coerceAtLeast(0)) {
                        placeable.place(0, -pullPx)
                    }
                }.aspectRatio(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
    ) {
        Box(Modifier.fillMaxSize().background(color), content = content)
        Box(
            Modifier
                .fillMaxWidth()
                .height(pull + 32.dp)
                .background(Brush.verticalGradient(0f to veil, 0.6f to veil.copy(alpha = veil.alpha * 0.6f), 1f to Color.Transparent)),
        )
    }
}

/**
 * The top bar color of a cover page: see-through while an edge-to-edge cover is under it
 * ([overCover]) or on a glass page, the page [tint] otherwise.
 */
@Composable
fun pageTopBarColor(
    tint: Color,
    glass: Boolean,
    overCover: Boolean,
): Color {
    val color by animateColorAsState(
        targetValue = if (glass || overCover) Color.Transparent else tint,
        animationSpec = tween(200),
        label = "pageTopBar",
    )
    return color
}

/** How much of the glass behind the top bar shows: none while an edge-to-edge cover is under it. */
@Composable
fun pageTopBarGlassAlpha(overCover: Boolean): Float {
    val alpha by animateFloatAsState(if (overCover) 0f else 1f, tween(200), label = "pageTopBarGlass")
    return alpha
}
