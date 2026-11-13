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
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.InnerTubeCookieKey
import kotlinx.coroutines.flow.first
import timber.log.Timber
import com.metrolist.music.constants.AutoDownloadedSongIdsKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val automaticDownloads =
        context.dataStore.data
            .map { it[AutoDownloadedSongIdsKey].orEmpty() }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit, hideVideoSongs) ->
                val (sortType, descending) = sortDesc
                when (playlist) {
                    "liked" -> database.likedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    // Only what the download button saved; "Automatically download library"
                    // downloads show in the Library list instead.
                    "downloaded" -> database.downloadedSongs(sortType, descending)
                        .combine(automaticDownloads) { songs, automatic ->
                            songs.filterNot { it.id in automatic }
                        }
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    "library" -> database.songs(sortType, descending)
                        .map { songs -> songs.filterNot { it.song.isEpisode } }
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    "uploaded" -> database.uploadedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    else -> kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }

    /** How many library songs are neither liked nor in one of your playlists. */
    suspend fun looseLibrarySongCount(): Int = database.looseLibrarySongCount()

    /**
     * TEMPORARY (asked 2026-11-13 to clear songs left over from an old account; remove later):
     * takes loose songs out of the app's library and, when logged in, out of the YouTube Music
     * library too, so the next sync doesn't bring them back.
     */
    fun removeLooseLibrarySongs(onDone: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = database.looseLibrarySongIds()
            val removed = database.removeLooseLibrarySongs()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone(removed) }
            val cookie = context.dataStore.data.map { it[InnerTubeCookieKey] }.first()
            val loggedIn = cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
            if (loggedIn) {
                ids.forEach { id ->
                    YouTube.toggleSongLibrary(id, addToLibrary = false)
                        .onFailure { Timber.w(it, "Failed to remove $id from the YouTube Music library") }
                    kotlinx.coroutines.delay(150)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            when (playlist) {
                "liked" -> syncUtils.syncLikedSongsSuspend()
                "uploaded" -> syncUtils.syncUploadedSongsSuspend()
            }
            _isRefreshing.value = false
        }
    }
}
