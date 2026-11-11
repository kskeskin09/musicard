/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import kotlin.random.Random

/**
 * The shuffle switch next to a list's play button. Like the one in Up next it only turns the
 * player's shuffle on or off; it never starts playback.
 */
@Composable
fun ShuffleModeButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val shuffleOn by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val background by animateColorAsState(
        if (shuffleOn) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        },
        label = "shuffleBackground",
    )
    val tint by animateColorAsState(
        if (shuffleOn) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
        label = "shuffleTint",
    )
    Surface(
        onClick = { playerConnection.player.shuffleModeEnabled = !shuffleOn },
        enabled = enabled,
        shape = CircleShape,
        color = background,
        modifier = modifier.size(48.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = stringResource(R.string.shuffle),
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Where a list starts playing: a random song while shuffle is on, otherwise the first. */
fun shuffleAwareStartIndex(
    shuffleOn: Boolean,
    size: Int,
): Int = if (shuffleOn && size > 0) Random.nextInt(size) else 0
