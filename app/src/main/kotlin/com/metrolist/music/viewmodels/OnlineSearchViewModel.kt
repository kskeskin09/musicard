/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SearchRelevance
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.preferOfficialSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.SearchSpellCorrection
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.PreferOfficialSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val database: MusicDatabase,
) : ViewModel() {
    val query = SearchRoutes.decodeQuery(savedStateHandle.get<String>("query").orEmpty())
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    /**
     * Everything found for the query in one list, best match first (see [SearchRelevance]). Shown
     * under the top result instead of YouTube Music's shelves, which are filled even with items that
     * have little to do with the query.
     */
    var rankedResults by mutableStateOf<List<YTItem>?>(null)

    /** The large card on top: YouTube's top result, unless it clearly matches the query worse. */
    var topResult by mutableStateOf<YTItem?>(null)

    /** YouTube's spelling fix for the query ("Did you mean" / "Showing results for"), if any. */
    var spellCorrection by mutableStateOf<SearchSpellCorrection?>(null)

    // The "Music" chip shows everything playable: official songs and videos together. They come
    // from two separate searches, so each keeps its own "load more" token.
    private var musicSongContinuation: String? = null
    private var musicVideoContinuation: String? = null

    private val recordHistory get() = !context.dataStore.get(PauseSearchHistoryKey, false)

    private fun List<YTItem>.applyContentFilters(): List<YTItem> =
        filterExplicit(context.dataStore.get(HideExplicitKey, false))
            .filterVideoSongs(context.dataStore.get(HideVideoSongsKey, false))
            .filterYoutubeShorts(context.dataStore.get(HideYoutubeShortsKey, false))

    /**
     * Mixes the song results and the video results one by one, a song first, so neither kind is
     * pushed to the end. With "Prefer songs over videos" on, an official song is also moved in front
     * of a video of the same song.
     */
    private fun mixSongsAndVideos(
        songs: List<YTItem>,
        videos: List<YTItem>,
    ): List<YTItem> {
        val mixed = ArrayList<YTItem>(songs.size + videos.size)
        for (i in 0 until maxOf(songs.size, videos.size)) {
            songs.getOrNull(i)?.let(mixed::add)
            videos.getOrNull(i)?.let(mixed::add)
        }
        return mixed
            .distinctBy { it.id }
            .preferOfficialSongs(context.dataStore.get(PreferOfficialSongsKey, true))
    }

    private suspend fun loadMusicFilter(record: Boolean = recordHistory): Unit =
        coroutineScope {
            val songs = async { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG, recordSearchHistory = record) }
            // Only the first request records the search in the account history.
            // Skipped when the user hides video songs: they would all be filtered out anyway.
            val videos =
                async {
                    if (context.dataStore.get(HideVideoSongsKey, false)) {
                        null
                    } else {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
                    }
                }
            val songResult = songs.await().onFailure { reportException(it) }.getOrNull()
            val videoResult = videos.await()?.onFailure { reportException(it) }?.getOrNull()
            if (songResult == null && videoResult == null) return@coroutineScope
            musicSongContinuation = songResult?.continuation
            musicVideoContinuation = videoResult?.continuation
            val resolved =
                resolveSearchMetadata(songResult?.items.orEmpty() + videoResult?.items.orEmpty())
            val songCount = songResult?.items?.size ?: 0
            viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] =
                ItemsPage(
                    mixSongsAndVideos(
                        resolved.take(songCount).applyContentFilters(),
                        resolved.drop(songCount).applyContentFilters(),
                    ),
                    musicSongContinuation ?: musicVideoContinuation,
                )
        }

    private var musicLoadingMore = false

    private suspend fun loadMoreMusicFilter() {
        if (musicLoadingMore) return
        musicLoadingMore = true
        try {
            loadMoreMusicFilterPage()
        } finally {
            musicLoadingMore = false
        }
    }

    private suspend fun loadMoreMusicFilterPage(): Unit =
        coroutineScope {
            val filterValue = YouTube.SearchFilter.FILTER_SONG.value
            val viewState = viewStateMap[filterValue] ?: return@coroutineScope
            val songs = async { musicSongContinuation?.let { YouTube.searchContinuation(it).getOrNull() } }
            val videos = async { musicVideoContinuation?.let { YouTube.searchContinuation(it).getOrNull() } }
            val songResult = songs.await()
            val videoResult = videos.await()
            if (songResult == null && videoResult == null) {
                // Both ended (or failed): stop showing the loading row.
                musicSongContinuation = null
                musicVideoContinuation = null
                viewStateMap[filterValue] = viewState.copy(continuation = null)
                return@coroutineScope
            }
            musicSongContinuation = songResult?.continuation
            musicVideoContinuation = videoResult?.continuation
            val resolved =
                resolveSearchMetadata(songResult?.items.orEmpty() + videoResult?.items.orEmpty())
            val songCount = songResult?.items?.size ?: 0
            val newItems =
                mixSongsAndVideos(
                    resolved.take(songCount).applyContentFilters(),
                    resolved.drop(songCount).applyContentFilters(),
                )
            viewStateMap[filterValue] =
                ItemsPage(
                    (viewState.items + newItems).distinctBy { it.id },
                    musicSongContinuation ?: musicVideoContinuation,
                )
        }

    private suspend fun resolveSearchMetadata(items: List<YTItem>): List<YTItem> =
        coroutineScope {
            val knownDurations =
                items
                    .filterIsInstance<SongItem>()
                    .mapNotNull { song -> song.duration?.let { song.id to it } }
                    .toMap()
            val missingDurationIds =
                items
                    .filterIsInstance<SongItem>()
                    .filter { it.duration == null && it.id !in knownDurations }
                    .map { it.id }
                    .distinct()
            val resolvedArtists = async { YouTube.resolveArtistIds(items) }
            val fetchedDurations =
                async {
                    if (missingDurationIds.isEmpty()) {
                        emptyMap()
                    } else {
                        YouTube
                            .queue(videoIds = missingDurationIds)
                            .getOrDefault(emptyList())
                            .mapNotNull { song -> song.duration?.let { song.id to it } }
                            .toMap()
                    }
                }
            val durations = knownDurations + fetchedDurations.await()

            resolvedArtists.await().map { item ->
                if (item is SongItem && item.duration == null) {
                    item.copy(duration = durations[item.id])
                } else {
                    item
                }
            }
        }

    /**
     * The "All" view: YouTube's summary (for the top result card) and the first pages of the songs
     * and videos searches (the same ones the Music chip shows) load together, then everything is
     * ranked by how well it matches the query.
     */
    private suspend fun loadAllResults() {
        if (rankedResults != null) return
        coroutineScope {
            launch { loadSummaryPage() }
            launch {
                if (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] == null) {
                    // The summary request already records the search in the account history.
                    loadMusicFilter(record = false)
                }
            }
        }
        val summaryItems = summaryPage?.summaries.orEmpty().flatMap { it.items }
        val musicItems = viewStateMap[YouTube.SearchFilter.FILTER_SONG.value]?.items.orEmpty()
        if (summaryPage == null && musicItems.isEmpty()) return
        val correction = spellCorrection
        // "Did you mean X": the results above are still for what was typed, so X's results are
        // added to the pool too (YouTube already searched X when it says "Showing results for").
        val correctedItems =
            if (correction != null && !correction.autoCorrected) {
                YouTube
                    .searchSummary(correction.query)
                    .getOrNull()
                    ?.summaries
                    .orEmpty()
                    .flatMap { it.items }
                    .applyContentFilters()
            } else {
                emptyList()
            }
        val knownIds = runCatching { database.knownSongIds().toSet() }.getOrDefault(emptySet())
        val ranked =
            SearchRelevance
                .rank(
                    query,
                    summaryItems + musicItems + correctedItems,
                    alsoMatches = correction?.query,
                    knownIds = knownIds,
                ).preferOfficialSongs(context.dataStore.get(PreferOfficialSongsKey, true))
        topResult =
            SearchRelevance.pickTop(
                query,
                summaryPage?.summaries?.firstOrNull()?.items?.firstOrNull(),
                ranked,
                alsoMatches = correction?.query,
            )
        rankedResults = ranked
    }

    private suspend fun loadSummaryPage() {
        if (summaryPage == null) {
            YouTube
                .searchSummary(query, recordSearchHistory = !context.dataStore.get(PauseSearchHistoryKey, false))
                .onSuccess { page ->
                    spellCorrection = page.spellCorrection
                    val resolvedItems = resolveSearchMetadata(page.summaries.flatMap { it.items })
                    var offset = 0
                    val resolvedSummaries =
                        page.summaries.map { summary ->
                            val nextOffset = offset + summary.items.size
                            val resolvedSummary = summary.copy(items = resolvedItems.subList(offset, nextOffset))
                            offset = nextOffset
                            resolvedSummary
                        }
                    val resolvedPage = page.copy(summaries = resolvedSummaries)
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                    summaryPage =
                        resolvedPage
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts)
                            .preferOfficialSongs(context.dataStore.get(PreferOfficialSongsKey, true))
                }.onFailure {
                    reportException(it)
                }
        }
    }

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    loadAllResults()
                } else if (filter == YouTube.SearchFilter.FILTER_EPISODE) {
                    // The FILTER_EPISODE API returns episodes in a format that differs from the
                    // summary search: playlistItemData is absent and the subtitle structure is
                    // different, making reliable isEpisode detection fail for many items.
                    // Reuse the "Episodes" section from the summary page instead — it is already
                    // parsed correctly by fromMusicResponsiveListItemRenderer and guaranteed to
                    // show the same results as the episodes section in the "All" filter.
                    if (viewStateMap[filter.value] == null) {
                        loadSummaryPage()
                        summaryPage?.let { page ->
                            val episodes = page.summaries
                                .firstOrNull { it.title == "Episodes" }
                                ?.items
                                .orEmpty()
                            viewStateMap[filter.value] = ItemsPage(episodes, null)
                        }
                    }
                } else if (filter == YouTube.SearchFilter.FILTER_SONG) {
                    if (viewStateMap[filter.value] == null) {
                        loadMusicFilter()
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter, recordSearchHistory = !context.dataStore.get(PauseSearchHistoryKey, false))
                            .onSuccess { result ->
                                val resolvedItems = resolveSearchMetadata(result.items)
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        resolvedItems
                                            .distinctBy { it.id }
                                            .filterExplicit(hideExplicit)
                                            .filterVideoSongs(hideVideoSongs)
                                            .filterYoutubeShorts(hideYoutubeShorts),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val currentFilter = filter.value
        val filterValue = currentFilter?.value ?: return
        viewModelScope.launch {
            if (currentFilter == YouTube.SearchFilter.FILTER_SONG) {
                loadMoreMusicFilter()
                return@launch
            }
            val viewState = viewStateMap[filterValue] ?: return@launch
            val continuation = viewState.continuation ?: return@launch
            val searchResult =
                YouTube.searchContinuation(
                    continuation,
                    recordSearchHistory = !context.dataStore.get(PauseSearchHistoryKey, false),
                ).getOrNull() ?: return@launch
            val resolvedItems = resolveSearchMetadata(searchResult.items)
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val newItems = resolvedItems
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .filterYoutubeShorts(hideYoutubeShorts)
            viewStateMap[filterValue] = ItemsPage(
                (viewState.items + newItems).distinctBy { it.id },
                searchResult.continuation
            )
        }
    }
}
