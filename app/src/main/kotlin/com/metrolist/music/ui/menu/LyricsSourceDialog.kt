/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.viewmodels.LyricsMenuViewModel

/**
 * Opened by tapping the lyrics source under the lyrics. Searches every lyrics provider for the
 * current song and previews each result; tapping a result switches the song's lyrics to it.
 */
@Composable
fun LyricsSourceDialog(
    mediaMetadata: MediaMetadata,
    currentProvider: String?,
    onDismiss: () -> Unit,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var expandedItemIndex by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(mediaMetadata.id) {
        viewModel.search(
            mediaMetadata.id,
            mediaMetadata.title,
            mediaMetadata.artists.joinToString { it.name },
            mediaMetadata.duration,
            mediaMetadata.album?.title,
        )
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelSearch() }
    }

    ListDialog(onDismiss = onDismiss) {
        itemsIndexed(results) { index, result ->
            val isCurrent = result.providerName == currentProvider
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            database.query {
                                upsert(
                                    LyricsEntity(
                                        id = mediaMetadata.id,
                                        lyrics = result.lyrics,
                                        provider = result.providerName,
                                    ),
                                )
                            }
                            onDismiss()
                        }.padding(12.dp)
                        .animateContentSize(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.providerName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                        if (result.lyrics.startsWith("[")) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier =
                                    Modifier
                                        .padding(start = 4.dp)
                                        .size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = result.lyrics,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(
                    onClick = { expandedItemIndex = if (expandedItemIndex == index) -1 else index },
                ) {
                    Icon(
                        painter = painterResource(if (index == expandedItemIndex) R.drawable.expand_less else R.drawable.expand_more),
                        contentDescription = null,
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!isLoading && results.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}
