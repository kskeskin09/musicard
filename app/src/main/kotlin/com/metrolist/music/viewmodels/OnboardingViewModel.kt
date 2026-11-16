/**
 * Musicard Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.OnboardingDoneKey
import com.metrolist.music.constants.OnboardingPendingKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

/** The song shown (never played) during the first-run setup. */
data class OnboardingSong(
    val title: String,
    val artist: String,
    val albumTitle: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
)

/** The album page shown in the "Album pages" step. */
data class OnboardingAlbum(
    val title: String,
    val artist: String,
    val year: Int?,
    val thumbnailUrl: String?,
    val songs: List<Pair<String, Int>>,
)

/** One tile in the artist picker; [similarTo] is set for artists suggested after a pick. */
data class OnboardingArtist(
    val item: ArtistItem,
    val similarTo: String? = null,
)

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
) : ViewModel() {
    private val _song = MutableStateFlow(FallbackSong)
    val song: StateFlow<OnboardingSong> = _song.asStateFlow()

    // Synced lyrics as LRC text, or null while loading / when none were found.
    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics.asStateFlow()

    private val _album = MutableStateFlow<OnboardingAlbum?>(null)
    val album: StateFlow<OnboardingAlbum?> = _album.asStateFlow()

    private val _suggestions = MutableStateFlow<List<OnboardingArtist>>(emptyList())
    val suggestions: StateFlow<List<OnboardingArtist>> = _suggestions.asStateFlow()

    private val _suggestionsLoading = MutableStateFlow(true)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading.asStateFlow()

    // Picked artists, in the order they were picked.
    private val _selected = MutableStateFlow<List<ArtistItem>>(emptyList())
    val selected: StateFlow<List<ArtistItem>> = _selected.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ArtistItem>?>(null)
    val searchResults: StateFlow<List<ArtistItem>?> = _searchResults.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsStarted = false

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch(Dispatchers.IO) {
            val found =
                YouTube
                    .search("$SongTitle $SongArtist", YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    ?.filter { item ->
                        item.title.startsWith("There Is a Light", ignoreCase = true) &&
                            item.artists.any { it.name.equals(SongArtist, ignoreCase = true) }
                    }?.let { candidates ->
                        candidates.firstOrNull { it.album?.name.equals(AlbumTitle, ignoreCase = true) }
                            ?: candidates.firstOrNull()
                    }
            if (found == null) {
                _lyrics.value = ""
                return@launch
            }
            _song.value =
                OnboardingSong(
                    title = found.title,
                    artist = found.artists.firstOrNull()?.name ?: SongArtist,
                    albumTitle = found.album?.name ?: AlbumTitle,
                    thumbnailUrl = found.thumbnail.resize(1080, 1080),
                    durationMs = (found.duration ?: FallbackDurationSec) * 1000L,
                )
            launch {
                val fetched = runCatching { lyricsHelper.getLyrics(found.toMediaMetadata(), cancelOtherJobs = false) }.getOrNull()
                _lyrics.value =
                    fetched?.lyrics?.takeUnless { it == LyricsEntity.LYRICS_NOT_FOUND || it.isBlank() } ?: ""
            }
            found.album?.id?.let { albumId ->
                YouTube.album(albumId).onSuccess { page ->
                    _album.value =
                        OnboardingAlbum(
                            title = page.album.title,
                            artist = page.album.artists?.firstOrNull()?.name ?: SongArtist,
                            year = page.album.year,
                            thumbnailUrl = page.album.thumbnail.resize(1080, 1080),
                            songs = page.songs.map { it.title to (it.duration ?: 0) },
                        )
                }
            }
        }
    }

    /** Fills the artist picker the first time it opens: today's chart artists, then well-known ones. */
    fun loadSuggestions() {
        if (suggestionsStarted) return
        suggestionsStarted = true
        viewModelScope.launch(Dispatchers.IO) {
            val fromCharts =
                YouTube
                    .getChartsPage()
                    .getOrNull()
                    ?.sections
                    ?.flatMap { it.items }
                    ?.filterIsInstance<ArtistItem>()
                    ?.filter { it.thumbnail != null }
                    .orEmpty()
            if (fromCharts.isNotEmpty()) addSuggestions(fromCharts)
            if (fromCharts.size < 12) {
                val known =
                    SeedArtists
                        .map { name ->
                            async {
                                YouTube
                                    .search(name, YouTube.SearchFilter.FILTER_ARTIST)
                                    .getOrNull()
                                    ?.items
                                    ?.filterIsInstance<ArtistItem>()
                                    ?.firstOrNull()
                            }
                        }.awaitAll()
                        .filterNotNull()
                addSuggestions(known)
            }
            _suggestionsLoading.value = false
        }
    }

    private fun addSuggestions(items: List<ArtistItem>) {
        _suggestions.update { current ->
            val ids = current.map { it.item.id }.toMutableSet()
            current + items.filter { ids.add(it.id) }.map { OnboardingArtist(it) }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()
        if (value.isBlank()) {
            _searchResults.value = null
            return
        }
        searchJob =
            viewModelScope.launch(Dispatchers.IO) {
                delay(350)
                val results =
                    YouTube
                        .search(value.trim(), YouTube.SearchFilter.FILTER_ARTIST)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<ArtistItem>()
                        .orEmpty()
                _searchResults.value = results
            }
    }

    /**
     * Picks or unpicks [artist]. A new pick moves the artist into the suggestions (when it came
     * from a search) and puts artists like it right after it.
     */
    fun toggle(artist: ArtistItem) {
        val wasSelected = _selected.value.any { it.id == artist.id }
        if (wasSelected) {
            _selected.update { list -> list.filterNot { it.id == artist.id } }
            return
        }
        _selected.update { it + artist }
        if (_suggestions.value.none { it.item.id == artist.id }) {
            _suggestions.update { listOf(OnboardingArtist(artist)) + it }
        }
        if (_query.value.isNotEmpty()) onQueryChange("")
        viewModelScope.launch(Dispatchers.IO) {
            val similar =
                YouTube
                    .artist(artist.id)
                    .getOrNull()
                    ?.sections
                    ?.flatMap { section -> section.items.filterIsInstance<ArtistItem>() }
                    ?.filter { it.id != artist.id }
                    ?.take(6)
                    .orEmpty()
            if (similar.isEmpty()) return@launch
            _suggestions.update { current ->
                val ids = current.map { it.item.id }.toSet()
                val fresh = similar.filter { it.id !in ids }.map { OnboardingArtist(it, similarTo = artist.id) }
                if (fresh.isEmpty()) return@update current
                val at = current.indexOfFirst { it.item.id == artist.id }
                if (at < 0) {
                    current + fresh
                } else {
                    current.subList(0, at + 1) + fresh + current.subList(at + 1, current.size)
                }
            }
        }
    }

    /**
     * Ends the setup. With [saveArtists], the picked artists are added to the library (as
     * subscribed artists), where Home takes its suggestions from until there's listening history.
     */
    fun finish(saveArtists: Boolean) {
        val picked = if (saveArtists) _selected.value else emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            if (picked.isNotEmpty()) {
                val now = LocalDateTime.now()
                picked.forEachIndexed { index, artist ->
                    val entity =
                        ArtistEntity(
                            id = artist.id,
                            name = artist.title,
                            thumbnailUrl = artist.thumbnail?.resize(544, 544),
                            channelId = artist.channelId,
                            // Later picks get later times, so the library keeps the picking order.
                            bookmarkedAt = now.plusNanos(index * 1_000_000L),
                        )
                    withContext(Dispatchers.IO) {
                        runCatching {
                            database.insert(entity)
                            database.update(entity)
                        }
                    }
                }
            }
            context.safeDataStoreEdit {
                it[OnboardingDoneKey] = true
                it.remove(OnboardingPendingKey)
            }
        }
    }

    companion object {
        const val SongTitle = "There Is a Light That Never Goes Out"
        const val SongArtist = "The Smiths"
        const val AlbumTitle = "The Queen Is Dead"
        private const val FallbackDurationSec = 244

        private val FallbackSong =
            OnboardingSong(
                title = SongTitle,
                artist = SongArtist,
                albumTitle = AlbumTitle,
                thumbnailUrl = null,
                durationMs = FallbackDurationSec * 1000L,
            )

        private val SeedArtists =
            listOf(
                "The Smiths",
                "Radiohead",
                "Arctic Monkeys",
                "Taylor Swift",
                "The Weeknd",
                "Billie Eilish",
                "Coldplay",
                "Kendrick Lamar",
                "Daft Punk",
                "Lana Del Rey",
                "Tame Impala",
                "Dua Lipa",
                "Queen",
                "The Beatles",
                "Fleetwood Mac",
            )
    }
}
