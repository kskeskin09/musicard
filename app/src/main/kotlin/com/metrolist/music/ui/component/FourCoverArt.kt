/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** A list's cover made from the covers of its first four songs, 2×2 (one cover with fewer). */
@Composable
fun FourCoverArt(
    covers: List<String?>,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    corner: Dp = 16.dp,
) {
    // Fills the top of the page edge to edge when that setting is on.
    PageCoverFrame(
        modifier = modifier,
        size = size,
        corner = corner,
        shadow = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        pullUp = 16.dp,
    ) {
        if (covers.size >= 4) {
            Column(Modifier.fillMaxSize()) {
                listOf(0, 2).forEach { row ->
                    Row(Modifier.weight(1f)) {
                        listOf(row, row + 1).forEach { index ->
                            AsyncImage(
                                model = covers[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
            }
        } else {
            AsyncImage(
                model = covers.firstOrNull(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
