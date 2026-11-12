/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Text that always takes the width of the widest of [variants] (for example "Play" / "Pause" or
 * "Subscribe" / "Subscribed"), so a button doesn't change size when its label changes.
 */
@Composable
fun SteadyText(
    text: String,
    variants: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        variants.forEach { variant ->
            Text(
                text = variant,
                style = style,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                modifier = Modifier.alpha(0f),
            )
        }
        Text(
            text = text,
            style = style,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
        )
    }
}
