/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SearchRelevance
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.preferOfficialSongs
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.PreferOfficialSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.utils.RECENT_SEARCH_CONTENTS_SHOWN
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.recentSearchContents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        database: MusicDatabase,
    ) : ViewModel() {
        val query = MutableStateFlow("")
        private val _viewState = MutableStateFlow(SearchSuggestionViewState())
        val viewState = _viewState.asStateFlow()

        init {
            viewModelScope.launch {
                query
                    .flatMapLatest { query ->
                        if (query.isEmpty()) {
                            combine(database.searchHistory(), context.recentSearchContents()) { history, recent ->
                                SearchSuggestionViewState(
                                    history = history,
                                    recentContents = recent.take(RECENT_SEARCH_CONTENTS_SHOWN),
                                )
                            }
                        } else {
                            // Check if query is a YouTube URL
                            val parsedUrl = YouTubeUrlParser.parse(query)
                            if (parsedUrl != null) {
                                // Fetch content from YouTube URL
                                val parsedItem = fetchParsedUrlItem(parsedUrl)
                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = parsedItem?.let { listOf(it) } ?: emptyList(),
                                            parsedUrlItem = parsedItem,
                                            isUrlQuery = true,
                                        )
                                    }
                            } else {
                                val result = YouTube.searchSuggestions(query).getOrNull()
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                val preferOfficialSongs = context.dataStore.get(PreferOfficialSongsKey, true)

                                val recommended =
                                    result
                                        ?.recommendedItems
                                        ?.distinctBy { it.id }
                                        ?.filterExplicit(hideExplicit)
                                        ?.filterVideoSongs(hideVideoSongs)
                                        ?.preferOfficialSongs(preferOfficialSongs)
                                        .orEmpty()

                                // YouTube's own suggestion box gives only a handful of items, so more
                                // matching results are added below them shortly after typing stops.
                                val moreRecommended =
                                    flow<List<YTItem>> {
                                        emit(emptyList())
                                        delay(MORE_RECOMMENDED_DELAY_MS)
                                        emit(
                                            moreRecommendedItems(query)
                                                .filterExplicit(hideExplicit)
                                                .filterVideoSongs(hideVideoSongs)
                                                .preferOfficialSongs(preferOfficialSongs),
                                        )
                                    }

                                // "Suggestions": up to 5 rows, past searches first, then YouTube's.
                                // "Recommended": YouTube's picks first, then more matching results.
                                combine(
                                    database
                                        .searchHistory(query)
                                        .map { it.take(MAX_SUGGESTIONS) },
                                    moreRecommended,
                                ) { history, more ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions =
                                            result
                                                ?.queries
                                                ?.filter { suggestionQuery ->
                                                    history.none { it.query == suggestionQuery }
                                                }.orEmpty()
                                                .take(MAX_SUGGESTIONS - history.size),
                                        items =
                                            (recommended + more)
                                                .distinctBy { it.id }
                                                .take(MAX_RECOMMENDED),
                                    )
                                }
                            }
                        }
                    }.collect {
                        _viewState.value = it
                    }
            }
        }

        /** Search results that match [query], best first, without podcasts, episodes and profiles. */
        private suspend fun moreRecommendedItems(query: String): List<YTItem> {
            val page = YouTube.searchSummary(query).getOrNull() ?: return emptyList()
            val items =
                page.summaries
                    .flatMap { it.items }
                    .filterNot { item ->
                        when (item) {
                            is SongItem -> item.isEpisode
                            is ArtistItem -> item.isProfile
                            is PlaylistItem -> item.isPodcast
                            is AlbumItem -> false
                            else -> true
                        }
                    }
            return SearchRelevance
                .rank(query, items, alsoMatches = page.spellCorrection?.query)
                .filter { SearchRelevance.coverage(query, it) > 0.0 || page.spellCorrection != null }
        }

        private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? =
            when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    // Use next() to get the song details from a video ID
                    YouTube
                        .next(WatchEndpoint(videoId = parsedUrl.id))
                        .getOrNull()
                        ?.items
                        ?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Playlist -> {
                    // Fetch playlist details
                    YouTube
                        .playlist(parsedUrl.id)
                        .getOrNull()
                        ?.playlist
                }

                is YouTubeUrlParser.ParsedUrl.Album -> {
                    // For albums, we need to get the browseId from the playlist
                    // First, try to get the album page
                    val albumResult = YouTube.album("MPREb_${parsedUrl.id}")
                    if (albumResult.isSuccess) {
                        albumResult.getOrNull()?.album
                    } else {
                        // If that fails, treat it as a playlist
                        YouTube
                            .playlist(parsedUrl.id)
                            .getOrNull()
                            ?.playlist
                    }
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    // Fetch artist details
                    if (parsedUrl.id.startsWith("MPRE")) {
                        // It's a browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    } else {
                        // It's a channel ID, we need to find the browse ID
                        // For now, try using the channel ID as browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    }
                }
            }
    }

private const val MAX_SUGGESTIONS = 5
private const val MAX_RECOMMENDED = 30
private const val MORE_RECOMMENDED_DELAY_MS = 350L

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    /** Items opened from search lately; shown only while the search box is empty. */
    val recentContents: List<YTItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val parsedUrlItem: YTItem? = null,
    val isUrlQuery: Boolean = false,
)
