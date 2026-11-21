/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    /**
     * Moods and genres picked for this listener, shown as "For You" above Genres on the Search tab.
     * Always 0, 2 or 4 tiles so the two-column grid never has a half row.
     */
    val forYou = MutableStateFlow<List<MoodAndGenres.Item>>(emptyList())

    init {
        viewModelScope.launch {
            // YouTube's Home chips are its own pick of moods/genres for this account, best first.
            // Home has usually loaded them already; only load Home here when it hasn't.
            val chipsJob =
                async(Dispatchers.IO) {
                    homeChipTitles.value
                        ?: withTimeoutOrNull(8_000) { YouTube.home().getOrNull()?.chips?.map { it.title } }
                            ?.also { homeChipTitles.value = it }
                        ?: emptyList()
                }
            YouTube
                .moodAndGenres()
                .onSuccess { sections ->
                    // Show For You together with Genres: wait a little for the chips if they
                    // aren't here yet, then fall back to listening history alone.
                    val chips = withTimeoutOrNull(CHIPS_WAIT_MS) { chipsJob.await() }
                    forYou.value = pickForYou(sections, chips.orEmpty())
                    moodAndGenres.value = sections
                    // Chips that come later are kept for the next time the page opens;
                    // the tiles on screen don't move.
                }.onFailure {
                    chipsJob.cancel()
                    reportException(it)
                }
        }
    }

    private suspend fun pickForYou(
        sections: List<MoodAndGenres>,
        chipTitles: List<String>,
    ): List<MoodAndGenres.Item> {
        val tiles = sections.flatMap { it.items }.distinctBy { it.endpoint.browseId + it.endpoint.params }
        if (tiles.isEmpty()) return emptyList()

        // Words from what the listener played most in the last 90 days (song, album and artist names).
        val listened =
            runCatching {
                withTimeoutOrNull(3_000) {
                    database
                        .mostPlayedSongs(LocalDateTime.now().minusDays(90), limit = 80)
                        .firstOrNull()
                }
            }.getOrNull().orEmpty()
        val listenedText =
            listened.joinToString(" ") { song ->
                buildString {
                    append(song.song.title)
                    append(' ')
                    append(song.song.albumName.orEmpty())
                    song.artists.forEach { append(' ').append(it.name) }
                }
            }
        val listenedWords = words(listenedText).groupingBy { it }.eachCount()

        val chips = chipTitles.map(::normalize).filter { it.isNotBlank() }
        val scored =
            tiles.mapNotNull { tile ->
                val name = normalize(tile.title)
                var score = 0.0
                val chipIndex = chips.indexOfFirst { chip -> chip == name || chip.contains(name) || name.contains(chip) }
                if (chipIndex >= 0) score += 100.0 - chipIndex
                val tileWords = words(tile.title).filter { it.length >= 3 && it !in StopWords }
                score += tileWords.sumOf { (listenedWords[it] ?: 0).coerceAtMost(10) } * 4.0
                if (score > 0) tile to score else null
            }.sortedByDescending { it.second }
                .map { it.first }

        val count = (scored.size.coerceAtMost(4) / 2) * 2
        return scored.take(count)
    }

    private fun normalize(text: String) = words(text).joinToString(" ")

    private fun words(text: String): List<String> =
        text
            .lowercase(Locale.ROOT)
            .replace("&", " ")
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }

    companion object {
        /** The latest Home chip titles, filled by HomeViewModel when Home loads. */
        val homeChipTitles = MutableStateFlow<List<String>?>(null)

        private const val CHIPS_WAIT_MS = 1_500L
        private val StopWords = setOf("and", "the", "music", "ve", "müzik", "for", "with")
    }
}
