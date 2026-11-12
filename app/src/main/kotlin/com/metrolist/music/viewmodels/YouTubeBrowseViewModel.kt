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
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.BrowseResult
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    /**
     * The genre's new releases for the New filter, loaded the first time it is picked. Genre pages
     * rarely have a "new releases" shelf, so these come from YouTube Music search: recent albums and
     * singles for the genre (newest year first) plus the official "New ..." playlists for it.
     * null while loading, empty when nothing recent was found.
     */
    val newItems = MutableStateFlow<List<YTItem>?>(null)
    private var newLoading = false

    fun loadNew(genre: String) {
        if (newLoading || newItems.value != null || genre.isBlank()) return
        newLoading = true
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val albums =
                listOf("$genre new releases", "new $genre $thisYear")
                    .map { query ->
                        async { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items.orEmpty() }
                    }.awaitAll()
                    .flatten()
                    .filterIsInstance<AlbumItem>()
                    .filter { (it.year ?: 0) >= thisYear - 1 }
                    .distinctBy { it.id }
                    .sortedByDescending { it.year ?: 0 }
            val playlists =
                YouTube
                    .search("new $genre", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()
                    ?.items
                    .orEmpty()
                    .filterIsInstance<PlaylistItem>()
                    .filter { pl ->
                        val t = pl.title.lowercase()
                        listOf("new", "fresh", "release", "yeni").any { it in t }
                    }.take(6)
            newItems.value = (playlists + albums).filterExplicit(hideExplicit)
            newLoading = false
        }
    }

    init {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
