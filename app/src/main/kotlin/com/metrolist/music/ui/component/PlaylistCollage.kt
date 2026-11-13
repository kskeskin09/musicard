/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.sortPlaylistSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * The covers for a playlist's 2×2 collage: its first four songs in the order its page shows them
 * (the chosen sort), so the library shows the same collage as the playlist page. Null when the
 * playlist has its own cover.
 */
@Composable
fun rememberPlaylistCollage(playlist: Playlist): List<String?>? {
    if (playlist.coverUrl != null) return null
    val database = LocalDatabase.current
    val sortType by rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val sortDescending by rememberPreference(PlaylistSongSortDescendingKey, true)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, false)
    // The first four by position, known right away; replaced once the sorted songs are read.
    val preview = playlist.songThumbnails.filterNotNull().take(4)
    val collage by produceState<List<String?>>(preview, playlist.id, sortType, sortDescending, hideVideoSongs) {
        database
            .playlistSongs(playlist.id)
            .map { songs ->
                sortPlaylistSongs(songs, sortType, sortDescending, hideVideoSongs)
                    .take(4)
                    .map { it.song.song.thumbnailUrl }
            }.distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .collect { value = it }
    }
    return collage
}

/** A 2×2 collage of [covers]; squares without a cover stay black. */
@Composable
fun PlaylistCollageGrid(
    covers: List<String?>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(Color.Black)) {
        listOf(0, 2).forEach { row ->
            Row(Modifier.weight(1f)) {
                listOf(row, row + 1).forEach { index ->
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        val cover = covers.getOrNull(index)
                        if (cover != null) {
                            AsyncImage(
                                model = cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
