/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.metrolist.music.constants.PageBackgroundKey
import com.metrolist.music.constants.PlayerCardBackground
import com.metrolist.music.utils.LocalBatterySaver
import com.metrolist.music.utils.rememberEnumPreference

/**
 * Whether album and playlist pages show the blurred cover ("Glass") instead of an even color
 * from it ("Classic"). Battery saver's blur switch turns it back to Classic.
 */
@Composable
fun rememberPageGlass(): Boolean {
    val pageBackground by rememberEnumPreference(PageBackgroundKey, defaultValue = PlayerCardBackground.CLASSIC)
    return pageBackground == PlayerCardBackground.GLASS && !LocalBatterySaver.current.blur
}

/**
 * The cover at [url], blurred across the whole page and washed toward the theme so its text
 * stays readable: darkened in dark mode, lightened in light mode. It does not scroll.
 *
 * With [topBarOnly], only the part behind the top bar is drawn. Placed between the list and the
 * top bar, it hides the list scrolling under the bar while looking exactly like the page behind.
 */
@Composable
fun PageGlassBackground(
    url: String?,
    modifier: Modifier = Modifier,
    topBarOnly: Boolean = false,
) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = LocalDensity.current
    val barHeight = WindowInsets.statusBars.getTop(density) + with(density) { 64.dp.toPx() }
    // Modifier.blur needs Android 12; older versions get a tiny image stretched over the page,
    // which smears the cover into soft color areas as well.
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val wash = if (dark) Color.Black else MaterialTheme.colorScheme.background
    val washStops =
        if (dark) {
            arrayOf(0f to wash.copy(alpha = 0.3f), 0.55f to wash.copy(alpha = 0.4f), 1f to wash.copy(alpha = 0.55f))
        } else {
            arrayOf(0f to wash.copy(alpha = 0.5f), 0.55f to wash.copy(alpha = 0.58f), 1f to wash.copy(alpha = 0.66f))
        }
    AnimatedContent(
        targetState = url,
        transitionSpec = { fadeIn(tween(600)).togetherWith(fadeOut(tween(600))) },
        label = "pageGlass",
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (topBarOnly) {
                        Modifier.drawWithContent {
                            clipRect(bottom = barHeight) { this@drawWithContent.drawContent() }
                        }
                    } else {
                        Modifier
                    },
                ),
    ) { target ->
        if (target == null) {
            Box(Modifier.fillMaxSize())
            return@AnimatedContent
        }
        Box(Modifier.fillMaxSize()) {
            val imageSize = if (canBlur) 96 else 6
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(target)
                        .size(imageSize, imageSize)
                        .allowHardware(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                        }.blur(90.dp),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colorStops = washStops)),
            )
        }
    }
}
