/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import com.metrolist.music.utils.FuzzySearch
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result =
        combine(
            query,
            filter,
            context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
        ) { query, filter, hideVideoSongs ->
            Triple(query, filter, hideVideoSongs)
        }.flatMapLatest { (query, filter, hideVideoSongs) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                // Everything in the library, ranked by a forgiving match (typos, accents, word
                // order), so the closest results come first.
                val songs =
                    database.searchSongs("").map { all ->
                        val visible = if (hideVideoSongs) all.filter { !it.song.isVideo } else all
                        FuzzySearch.rank(visible, query) { song ->
                            listOf(song.song.title, song.song.title + " " + song.artists.joinToString(" ") { it.name }, song.album?.title)
                        }
                    }
                val albums =
                    database.searchAlbums("").map { all ->
                        FuzzySearch.rank(all, query) { album ->
                            listOf(album.album.title, album.album.title + " " + album.artists.joinToString(" ") { it.name })
                        }
                    }
                val artists = database.searchArtists("").map { all -> FuzzySearch.rank(all, query) { listOf(it.artist.name) } }
                val playlists = database.searchPlaylists("").map { all -> FuzzySearch.rank(all, query) { listOf(it.playlist.name) } }
                when (filter) {
                    LocalFilter.ALL ->
                        combine(songs, albums, artists, playlists) { s, al, ar, p ->
                            s.take(PREVIEW_SIZE) + al.take(PREVIEW_SIZE) + ar.take(PREVIEW_SIZE) + p.take(PREVIEW_SIZE)
                        }

                    LocalFilter.SONG -> songs
                    LocalFilter.ALBUM -> albums
                    LocalFilter.ARTIST -> artists
                    LocalFilter.PLAYLIST -> playlists
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                        list.groupBy {
                            when (it) {
                                is Song -> LocalFilter.SONG
                                is Album -> LocalFilter.ALBUM
                                is Artist -> LocalFilter.ARTIST
                                is Playlist -> LocalFilter.PLAYLIST
                            }
                        },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
