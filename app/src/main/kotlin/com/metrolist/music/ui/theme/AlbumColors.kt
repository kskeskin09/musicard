/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a color taken from cover art into a calm background color: less saturated, and kept in a
 * lightness range that suits the theme (a little darker in dark mode, a little lighter in light
 * mode). White text stays readable on both.
 */
fun albumBackgroundColor(
    base: Color,
    darkTheme: Boolean,
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.8f).coerceAtMost(0.55f)
    hsl[2] = if (darkTheme) hsl[2].coerceIn(0.18f, 0.28f) else hsl[2].coerceIn(0.34f, 0.44f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/** The dominant color of the image at [url], or null while it loads or if it can't be read. */
@Composable
fun rememberCoverColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        color = loadCoverColor(context, url)
    }
    return color
}

private suspend fun loadCoverColor(
    context: Context,
    url: String,
): Color? {
    val request =
        ImageRequest
            .Builder(context)
            .data(url)
            .size(100, 100)
            .allowHardware(false)
            .build()
    val bitmap = runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        val palette =
            Palette
                .from(bitmap)
                .maximumColorCount(8)
                .resizeBitmapArea(100 * 100)
                .generate()
        Color(palette.rankedColors(1, android.graphics.Color.DKGRAY).first())
    }
}
