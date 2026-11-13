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

/**
 * Three free sources that answer with Apple-style TTML (word-by-word timing):
 * - BetterLyrics Portato: the BetterLyrics server's QQ Music route.
 * - Unison: a second lyrics service from the BetterLyrics developer.
 * - lrc.red: a TTML archive keyed by ISRC, which BiniLyrics also points at.
 */
internal object CommunityTtmlLyrics {
    private val ISRC = Regex("""^[A-Z]{2}[A-Z0-9]{3}\d{7}$""")

    suspend fun portato(title: String, artist: String, durationSec: Int, album: String?): Result<String> = runCatching {
        val response = lyricsHttp.get("https://lyrics-api.boidu.dev/qq/getLyrics") {
            header("Accept", "application/json")
            parameter("s", title)
            parameter("a", artist)
            if (durationSec > 0) parameter("d", durationSec)
            if (!album.isNullOrBlank()) parameter("al", album)
        }
        if (!response.status.isSuccess()) throw IllegalStateException("Portato answered ${response.status}")
        val body = response.bodyAsText()
        val json = parseJsonOrNull(body)
        val ttml = json["ttml"].str() ?: json["lyrics"].str() ?: body.takeIf { it.trimStart().startsWith("<") }
        ttml?.let(::ttmlOrLrc) ?: throw IllegalStateException("Portato has no lyrics for this song")
    }

    suspend fun unison(title: String, artist: String, durationSec: Int, album: String?): Result<String> = runCatching {
        val response = lyricsHttp.get("https://unison.boidu.dev/lyrics") {
            header("Accept", "application/json")
            parameter("song", title)
            parameter("artist", artist)
            if (!album.isNullOrBlank()) parameter("album", album)
            if (durationSec > 0) parameter("duration", durationSec)
        }
        if (!response.status.isSuccess()) throw IllegalStateException("Unison answered ${response.status}")
        val json = parseJsonOrNull(response.bodyAsText())
        if (json["success"].str() == "false") throw IllegalStateException("Unison has no lyrics for this song")
        val data = json["data"]
        val text = data["lyrics"].str()?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Unison has no lyrics for this song")
        when {
            data["format"].str().equals("ttml", ignoreCase = true) || text.startsWith("<") ->
                ttmlToAppLrc(text) ?: throw IllegalStateException("Unison sent unreadable TTML")
            else -> text
        }
    }

    suspend fun lrcRed(title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        val response = lyricsHttp.get("https://lrc.red/search.json") {
            header("Accept", "application/json")
            parameter("q", "$title ${primaryArtist(artist)}".trim())
        }
        if (!response.status.isSuccess()) throw IllegalStateException("lrc.red answered ${response.status}")
        val hits = parseJsonOrNull(response.bodyAsText())["hits"].arr().orEmpty()
        val match = hits.mapNotNull { hit ->
            val isrc = hit["isrc"].str()?.trim()?.uppercase()?.takeIf { ISRC.matches(it) } ?: return@mapNotNull null
            TrackCandidate(
                title = hit["title"].str() ?: return@mapNotNull null,
                artists = listOfNotNull(hit["artist"].str()),
                durationSec = hit["duration"].double()?.toInt() ?: 0,
                ref = isrc,
            )
        }.bestMatch(title, artist, durationSec, maxDurationDiff = 3)
            ?: throw IllegalStateException("No matching lrc.red song")
        val document = lyricsHttp.get("https://lrc.red/s/${match.ref}.ttml")
        if (!document.status.isSuccess()) throw IllegalStateException("lrc.red has no lyrics for this song")
        ttmlToAppLrc(document.bodyAsText()) ?: throw IllegalStateException("lrc.red sent unreadable TTML")
    }

    private fun ttmlOrLrc(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("<")) ttmlToAppLrc(trimmed) else trimmed
    }
}
