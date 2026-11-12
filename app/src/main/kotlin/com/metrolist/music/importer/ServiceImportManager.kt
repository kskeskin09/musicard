/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import android.content.Context
import android.net.Uri
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class MatchQuality { MATCHED, UNSURE, NOT_FOUND }

data class MatchReportItem(
    val original: String,
    val found: String?,
    val quality: MatchQuality,
    /** What to type in search to fix it by hand. */
    val query: String,
)

data class ImportOptions(
    val playlists: Boolean = true,
    val likedSongs: Boolean = true,
    val albums: Boolean = true,
    val artists: Boolean = true,
)

sealed interface ServiceImportState {
    data object Idle : ServiceImportState
    data object Reading : ServiceImportState
    data object Unsupported : ServiceImportState
    data class Ready(val bundle: ImportBundle) : ServiceImportState
    data class Running(
        val step: String,
        val done: Int,
        val total: Int,
        val matched: Int,
        val unsure: Int,
        val notFound: Int,
    ) : ServiceImportState
    data class Finished(
        val matched: Int,
        val unsure: Int,
        val notFound: Int,
        val playlistsCreated: Int,
        val albumsSaved: Int,
        val artistsFollowed: Int,
        val problems: List<MatchReportItem>,
    ) : ServiceImportState
}

/**
 * Runs an import from another service outside any screen, so it keeps going when the user leaves the page.
 * Every song is looked up on YouTube Music and the best result is kept; weak matches are kept too but listed
 * in the report so the user can check them.
 */
@Singleton
class ServiceImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<ServiceImportState>(ServiceImportState.Idle)
    val state: StateFlow<ServiceImportState> = _state.asStateFlow()
    private var job: Job? = null

    fun read(uri: Uri) {
        if (_state.value is ServiceImportState.Running) return
        _state.value = ServiceImportState.Reading
        job = scope.launch {
            _state.value = runCatching { ServiceImportParser.parse(context, uri) }
                .fold(
                    onSuccess = { ServiceImportState.Ready(it) },
                    onFailure = {
                        if (it !is ServiceImportParser.UnsupportedFileException) reportException(it)
                        ServiceImportState.Unsupported
                    },
                )
        }
    }

    fun reset() {
        if (_state.value is ServiceImportState.Running) return
        _state.value = ServiceImportState.Idle
    }

    fun cancel() {
        job?.cancel()
        _state.value = ServiceImportState.Idle
    }

    fun start(bundle: ImportBundle, options: ImportOptions, stepNames: StepNames) {
        if (_state.value is ServiceImportState.Running) return
        job = scope.launch { run(bundle, options, stepNames) }
    }

    /** Localized names for the steps, read by the screen before starting. */
    data class StepNames(val songs: String, val albums: String, val artists: String)

    private val matched = AtomicInteger()
    private val unsure = AtomicInteger()
    private val notFound = AtomicInteger()
    private val done = AtomicInteger()
    private val problems = ConcurrentHashMap<String, MatchReportItem>()
    private val cache = ConcurrentHashMap<String, MatchResult>()

    private data class MatchResult(val songId: String?, val quality: MatchQuality, val found: String?)

    private suspend fun run(bundle: ImportBundle, options: ImportOptions, names: StepNames) {
        matched.set(0); unsure.set(0); notFound.set(0); done.set(0)
        problems.clear(); cache.clear()

        val playlists = if (options.playlists) bundle.playlists else emptyList()
        val liked = if (options.likedSongs) bundle.likedSongs else emptyList()
        val albums = if (options.albums) bundle.albums else emptyList()
        val artists = if (options.artists) bundle.artists else emptyList()
        val uniqueSongs = (playlists.flatMap { it.tracks } + liked).distinctBy { it.key }
        val total = uniqueSongs.size + albums.size + artists.size
        var playlistsCreated = 0
        var albumsSaved = 0
        var artistsFollowed = 0

        fun publish(step: String) {
            _state.value = ServiceImportState.Running(step, done.get(), total, matched.get(), unsure.get(), notFound.get())
        }

        try {
            // 1) Songs: look every distinct one up once, then build the playlists and likes from the cache.
            publish(names.songs)
            val semaphore = Semaphore(8)
            coroutineScope {
                uniqueSongs.map { track ->
                    async {
                        semaphore.withPermit { matchSong(track) }
                        done.incrementAndGet()
                        publish(names.songs)
                    }
                }.awaitAll()
            }

            playlists.forEach { source ->
                val ids = source.tracks.mapNotNull { cache[it.key]?.songId }.distinct()
                if (ids.isEmpty()) return@forEach
                val entity = PlaylistEntity(name = source.name)
                database.insert(entity)
                val playlist = database.playlist(entity.id).firstOrNull() ?: return@forEach
                database.addSongsToPlaylist(playlist, ids.map { it to null })
                playlistsCreated++
            }

            if (liked.isNotEmpty()) {
                val now = LocalDateTime.now()
                // Keep the source order: the first song in the file becomes the most recently liked one.
                liked.mapNotNull { cache[it.key]?.songId }.distinct().forEachIndexed { index, id ->
                    val song = database.songEntity(id) ?: return@forEachIndexed
                    if (!song.liked) {
                        // Only in Musicard; it is not sent to the YouTube account, so a big import can't flood it.
                        database.update(
                            song.copy(
                                liked = true,
                                likedDate = now.minusSeconds(index.toLong()),
                                inLibrary = song.inLibrary ?: now,
                            ),
                        )
                    }
                }
            }

            // 2) Albums
            if (albums.isNotEmpty()) publish(names.albums)
            albums.forEach { album ->
                if (saveAlbum(album)) albumsSaved++
                done.incrementAndGet()
                publish(names.albums)
            }

            // 3) Artists
            if (artists.isNotEmpty()) publish(names.artists)
            artists.forEach { artist ->
                if (followArtist(artist)) artistsFollowed++
                done.incrementAndGet()
                publish(names.artists)
            }

            _state.value = ServiceImportState.Finished(
                matched = matched.get(),
                unsure = unsure.get(),
                notFound = notFound.get(),
                playlistsCreated = playlistsCreated,
                albumsSaved = albumsSaved,
                artistsFollowed = artistsFollowed,
                problems = problems.values.sortedWith(compareByDescending<MatchReportItem> { it.quality }.thenBy { it.original }),
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            reportException(e)
            _state.value = ServiceImportState.Finished(
                matched.get(), unsure.get(), notFound.get(), playlistsCreated, albumsSaved, artistsFollowed,
                problems.values.toList(),
            )
        }
    }

    private suspend fun matchSong(track: ImportTrack) {
        if (cache.containsKey(track.key)) return
        val artistText = track.artists.joinToString(" ")
        val query = listOf(track.title, artistText).filter { it.isNotBlank() }.joinToString(" ")
        val original = if (artistText.isBlank()) track.title else "${track.title} · ${track.artists.joinToString(", ")}"

        val candidates = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            ?.items?.filterIsInstance<SongItem>()?.take(5).orEmpty()
        val best = candidates.maxByOrNull { score(track, it) }
        val result = if (best == null) {
            MatchResult(null, MatchQuality.NOT_FOUND, null)
        } else {
            val quality = if (score(track, best) >= 3) MatchQuality.MATCHED else MatchQuality.UNSURE
            runCatching { database.insert(best.toMediaMetadata()) }.onFailure { Timber.e(it) }
            MatchResult(best.id, quality, "${best.title} · ${best.artists.joinToString(", ") { it.name }}")
        }
        cache[track.key] = result
        when (result.quality) {
            MatchQuality.MATCHED -> matched.incrementAndGet()
            MatchQuality.UNSURE -> unsure.incrementAndGet()
            MatchQuality.NOT_FOUND -> notFound.incrementAndGet()
        }
        if (result.quality != MatchQuality.MATCHED) {
            problems[track.key] = MatchReportItem(original, result.found, result.quality, query)
        }
    }

    /** 0–4: up to 2 points for the title, up to 2 for the artist. */
    private fun score(track: ImportTrack, item: SongItem): Int {
        var score = 0
        val wantTitle = clean(track.title)
        val gotTitle = clean(item.title)
        when {
            wantTitle == gotTitle -> score += 2
            wantTitle.isNotEmpty() && (gotTitle.contains(wantTitle) || wantTitle.contains(gotTitle)) -> score += 1
        }
        val gotArtists = item.artists.map { clean(it.name) }
        val wantArtists = track.artists.map(::clean)
        if (wantArtists.isEmpty()) {
            score += 1
        } else {
            val hits = wantArtists.count { want -> gotArtists.any { got -> got == want || got.contains(want) || want.contains(got) } }
            if (hits > 0) score += if (hits == wantArtists.size || gotArtists.firstOrNull() in wantArtists) 2 else 1
        }
        return score
    }

    private suspend fun saveAlbum(album: ImportAlbum): Boolean {
        val query = "${album.title} ${album.artist}".trim()
        val item = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
            ?.items?.filterIsInstance<AlbumItem>()
            ?.maxByOrNull { candidate ->
                (if (clean(candidate.title) == clean(album.title)) 2 else 0) +
                    (if (candidate.artists.orEmpty().any { clean(it.name) == clean(album.artist) }) 2 else 0)
            }
        if (item == null) {
            problems["album|$query"] = MatchReportItem("${album.title} · ${album.artist}", null, MatchQuality.NOT_FOUND, query)
            return false
        }
        return runCatching {
            if (database.albumEntity(item.browseId) == null) {
                YouTube.album(item.browseId).getOrNull()?.let { database.insert(it) }
            }
            val entity = database.albumEntity(item.browseId) ?: return false
            if (entity.bookmarkedAt == null) database.update(entity.copy(bookmarkedAt = LocalDateTime.now()))
            true
        }.getOrElse { reportException(it); false }
    }

    private suspend fun followArtist(name: String): Boolean {
        val item = YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
            ?.items?.filterIsInstance<ArtistItem>()
            ?.firstOrNull { clean(it.title) == clean(name) }
        if (item == null) {
            problems["artist|$name"] = MatchReportItem(name, null, MatchQuality.NOT_FOUND, name)
            return false
        }
        return runCatching {
            val existing = database.getArtistById(item.id)
            if (existing == null) {
                database.insert(
                    ArtistEntity(
                        id = item.id,
                        name = item.title,
                        thumbnailUrl = item.thumbnail,
                        channelId = item.channelId,
                        bookmarkedAt = LocalDateTime.now(),
                    ),
                )
            } else if (existing.bookmarkedAt == null) {
                database.update(existing.copy(bookmarkedAt = LocalDateTime.now()))
            }
            true
        }.getOrElse { reportException(it); false }
    }

    private fun clean(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            // "Song (Remastered 2011)", "Song - Live" and "feat." parts don't decide a match.
            .replace(Regex("\\s*[(\\[].*?[)\\]]"), "")
            .replace(Regex("\\s+-\\s+.*$"), "")
            .replace(Regex("\\b(feat|ft)\\.?\\s.*$"), "")
            .filter { it.isLetterOrDigit() }
}
