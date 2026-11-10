/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

// Fixed palettes: near-black / grouped-grey surfaces with a monochrome accent.
private val MusicardDarkColors =
    darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF3A3A3C),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF1C1C1E),
        secondary = Color(0xFFD1D1D6),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF2C2C2E),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFAEAEB2),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF2C2C2E),
        onTertiaryContainer = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF2C2C2E),
        onSurfaceVariant = Color(0xFF98989F),
        surfaceTint = Color(0xFFFFFFFF),
        inverseSurface = Color(0xFFF2F2F7),
        inverseOnSurface = Color(0xFF000000),
        error = Color(0xFFFF453A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF5C1512),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF636366),
        outlineVariant = Color(0xFF38383A),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF3A3A3C),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF141416),
        surfaceContainer = Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
    )

private val MusicardLightColors =
    lightColorScheme(
        primary = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5E5EA),
        onPrimaryContainer = Color(0xFF000000),
        inversePrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF3A3A3C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5E5EA),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFF636366),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE5E5EA),
        onTertiaryContainer = Color(0xFF000000),
        background = Color(0xFFF2F2F7),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFF2F2F7),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF6C6C70),
        surfaceTint = Color(0xFF000000),
        inverseSurface = Color(0xFF1C1C1E),
        inverseOnSurface = Color(0xFFFFFFFF),
        error = Color(0xFFFF3B30),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFFAEAEB2),
        outlineVariant = Color(0xFFD1D1D6),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE5E5EA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFE9E9EE),
        surfaceContainerHighest = Color(0xFFE0E0E6),
    )

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Musicard uses one fixed neutral palette for each theme instead of wallpaper / album colors,
    // so the app looks the same on every phone. [themeColor] is kept for callers but not used here.
    val baseColorScheme = if (darkTheme) MusicardDarkColors else MusicardLightColors

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
