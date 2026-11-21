/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.pages.UnavailableSong
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius

/** A greyed playlist row for a song YouTube Music can't play. It never starts playback. */
@Composable
fun UnavailableSongListItem(
    song: UnavailableSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = stringResource(R.string.song_unavailable_label)
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .alpha(0.45f),
        title = song.title,
        subtitle = listOfNotNull(unavailable, song.subtitle).joinToString(" • "),
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = song.thumbnail,
                isActive = false,
                isPlaying = false,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.song_unavailable),
                contentDescription = unavailable,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(20.dp),
            )
        },
    )
}
