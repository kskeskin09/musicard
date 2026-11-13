/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics.sources

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import kotlin.math.abs

/**
 * SimpMusic Lyrics: a free, community-filled lyrics database keyed by YouTube video id, which is
 * exactly what the app plays. No account needed.
 */
internal object SimpMusicLyrics {
    private const val BASE = "https://api-lyrics.simpmusic.org/v1"

    suspend fun getLyrics(videoId: String, title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        val byId = if (videoId.isNotBlank()) entries("$BASE/$videoId") else emptyList()
        val pick = pickEntry(byId, durationSec)
            ?: run {
                val found = entries("$BASE/search") { parameter("q", "$title ${primaryArtist(artist)}") }
                val match = found.mapNotNull { entry ->
                    val name = entry["songTitle"].str() ?: return@mapNotNull null
                    TrackCandidate(
                        title = name,
                        artists = listOfNotNull(entry["artistName"].str()),
                        durationSec = entry["durationSeconds"].long()?.toInt() ?: entry["duration"].long()?.toInt() ?: 0,
                        ref = entry,
                    )
                }.bestMatch(title, artist, durationSec, maxDurationDiff = 10)
                match?.ref
            }
            ?: throw IllegalStateException("SimpMusic has no lyrics for this song")
        lyricsOf(pick) ?: throw IllegalStateException("SimpMusic has no lyrics for this song")
    }

    private fun pickEntry(entries: List<JsonElement>, durationSec: Int): JsonElement? {
        val usable = entries.filter { lyricsOf(it) != null }
        if (durationSec <= 0) return usable.firstOrNull()
        return usable
            .map { it to (it["durationSeconds"].long() ?: it["duration"].long() ?: durationSec.toLong()) }
            .filter { abs(it.second - durationSec) <= 10 }
            .minByOrNull { abs(it.second - durationSec) }
            ?.first
    }

    /** The best lyrics in an entry: word-timed, then line-timed, then plain. */
    private fun lyricsOf(entry: JsonElement): String? =
        entry["richSyncLyrics"].str()?.takeIf { it.isNotBlank() }
            ?: entry["syncedLyrics"].str()?.takeIf { it.isNotBlank() }
            ?: entry["plainLyric"].str()?.takeIf { it.isNotBlank() }
            ?: entry["plainLyrics"].str()?.takeIf { it.isNotBlank() }

    private suspend fun entries(
        url: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): List<JsonElement> {
        val response = lyricsHttp.get(url) {
            header("Accept", "application/json")
            block()
        }
        if (!response.status.isSuccess()) return emptyList()
        val root = parseJsonOrNull(response.bodyAsText()) ?: return emptyList()
        if (root["success"].str() == "false") return emptyList()
        val data = root["data"]
        return data.arr()?.toList() ?: listOfNotNull(data.obj())
    }
}
