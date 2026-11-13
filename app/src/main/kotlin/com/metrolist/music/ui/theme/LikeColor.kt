/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The style of filled (liked) hearts, picked in Appearance settings: Classic (red) or Modern (white). */
enum class LikeIconColor {
    RED,
    WHITE,
}

/** True when liked hearts are red instead of the text color. */
val LocalLikeIconRed = staticCompositionLocalOf { false }

private val LikedRed = Color(0xFFF0455A)

/** The tint of a filled heart: red when chosen in settings, otherwise [default] (the text color). */
@Composable
fun likedTint(default: Color = LocalContentColor.current): Color = if (LocalLikeIconRed.current) LikedRed else default
