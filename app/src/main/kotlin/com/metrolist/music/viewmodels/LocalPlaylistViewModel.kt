/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.get<String>("playlistId")!!
    val playlist =
        database
            .playlist(playlistId)
            // Starts from the card that was tapped, so the page has its cover and name on the
            // first frame instead of opening empty while the database answers.
            .stateIn(viewModelScope, SharingStarted.Lazily, PlaylistPreviewCache[playlistId])

    private val _onlinePlaylist = MutableStateFlow<PlaylistItem?>(null)
    val onlinePlaylist: StateFlow<PlaylistItem?> = _onlinePlaylist
    val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            database.playlistSongs(playlistId),
            context.dataStore.data
                .map {
                    Triple(
                        it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM),
                        it[PlaylistSongSortDescendingKey] ?: true,
                        it[HideVideoSongsKey] ?: false
                    )
                }.distinctUntilChanged(),
        ) { songs, (sortType, sortDescending, hideVideoSongs) ->
            sortPlaylistSongs(songs, sortType, sortDescending, hideVideoSongs)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            val sortedSongs =
                playlistSongs.first().sortedWith(compareBy({ it.map.position }, { it.map.id }))
            database.transaction {
                sortedSongs.forEachIndexed { index, playlistSong ->
                    if (playlistSong.map.position != index) {
                        update(playlistSong.map.copy(position = index))
                    }
                }
            }
        }

        viewModelScope.launch {
            val localPlaylist = playlist.first { it != null }
            val browseId = localPlaylist?.playlist?.browseId
            if (browseId != null) {
                val page = withContext(Dispatchers.IO) {
                    YouTube.playlist(browseId).getOrNull()
                }
                val online = page?.playlist
                _onlinePlaylist.value = online
            }
        }
    }
}

/** The playlists last shown as cards, so a playlist page can open with them right away. */
object PlaylistPreviewCache {
    private val playlists = android.util.LruCache<String, com.metrolist.music.db.entities.Playlist>(300)

    operator fun get(id: String): com.metrolist.music.db.entities.Playlist? = playlists.get(id)

    fun put(playlist: com.metrolist.music.db.entities.Playlist) {
        playlists.put(playlist.id, playlist)
    }
}

/**
 * A playlist's songs in the order its page shows them: the chosen sort (and direction), without
 * videos when those are hidden. Used by the page and by the playlist's cover collage.
 */
fun sortPlaylistSongs(
    songs: List<PlaylistSong>,
    sortType: PlaylistSongSortType,
    sortDescending: Boolean,
    hideVideoSongs: Boolean,
): List<PlaylistSong> {
    val filteredSongs = if (hideVideoSongs) {
        songs.filter { !it.song.song.isVideo }
    } else {
        songs
    }
    return when (sortType) {
        PlaylistSongSortType.CUSTOM -> filteredSongs
        PlaylistSongSortType.CREATE_DATE -> filteredSongs.sortedBy { it.map.id }
        PlaylistSongSortType.NAME -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            filteredSongs.sortedWith(compareBy(collator) { it.song.song.title })
        }
        PlaylistSongSortType.ARTIST -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            filteredSongs.sortedWith(
                compareBy<PlaylistSong, String>(collator) { song -> song.song.artists.joinToString("") { it.name } }
                    .thenBy(collator) { it.song.album?.title.orEmpty() },
            )
        }

        PlaylistSongSortType.PLAY_TIME -> filteredSongs.sortedBy { it.song.song.totalPlayTime }
        // Last change to the song entry (file date, else when it was added to the library),
        // then the order it was added to the playlist.
        PlaylistSongSortType.MODIFIED_DATE ->
            filteredSongs.sortedWith(
                compareBy<PlaylistSong, java.time.LocalDateTime?>(nullsFirst()) {
                    it.song.song.dateModified ?: it.song.song.inLibrary
                }.thenBy { it.map.id },
            )
    }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
}
