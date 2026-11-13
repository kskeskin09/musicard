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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Musixmatch and Spotify lyrics through the PaxSenix lyrics server (the same server the
 * "Paxsenix" source already uses for Apple Music). No account needed.
 */
internal object PaxsenixExtraLyrics {
    private const val BASE = "https://lyrics.paxsenix.org"

    suspend fun musixmatch(title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        // Word timing first; when Musixmatch has none, the line-timed (or plain) lyrics.
        val types = listOf("word", "default")
        for (type in types) {
            val body = fetchText("/musixmatch/lyrics") {
                parameter("t", title)
                parameter("a", primaryArtist(artist))
                if (durationSec > 0) parameter("d", durationSec)
                parameter("type", type)
                parameter("format", "lrc")
            } ?: continue
            parsePayload(body)?.let { return@runCatching it }
        }
        throw IllegalStateException("Musixmatch has no lyrics for this song")
    }

    suspend fun spotify(title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        val results = fetchText("/spotify/search") { parameter("q", "$title ${primaryArtist(artist)}") }
            ?.let(::parseJsonOrNull)
            .let { it.arr() ?: it["results"].arr() ?: it["data"].arr() }
            ?: throw IllegalStateException("Spotify search failed")
        val match = results.mapNotNull { item ->
            val id = item["trackId"].str() ?: item["id"].str() ?: item["realId"].str() ?: return@mapNotNull null
            val name = item["name"].str() ?: item["title"].str() ?: return@mapNotNull null
            val artists = item["artistName"].str()?.let { listOf(it) }
                ?: item["artist"].str()?.let { listOf(it) }
                ?: item["artists"].arr()?.mapNotNull { it.str() ?: it["name"].str() }
                ?: emptyList()
            TrackCandidate(name, artists, durationSeconds(item["durationMs"] ?: item["duration"]), id)
        }.bestMatch(title, artist, durationSec) ?: throw IllegalStateException("No matching Spotify song")
        val body = fetchText("/spotify/lyrics") { parameter("id", match.ref) }
            ?: throw IllegalStateException("Spotify has no lyrics for this song")
        parsePayload(body) ?: throw IllegalStateException("Spotify has no lyrics for this song")
    }

    private suspend fun fetchText(path: String, block: io.ktor.client.request.HttpRequestBuilder.() -> Unit): String? {
        val response = lyricsHttp.get("$BASE$path") {
            header("Accept", "application/json")
            block()
        }
        if (!response.status.isSuccess()) return null
        return response.bodyAsText().trim().takeIf { it.isNotEmpty() }
    }

    /** "03:53", milliseconds or seconds, as seconds. */
    private fun durationSeconds(value: JsonElement?): Int {
        val text = value.str()?.trim() ?: return 0
        if (':' in text) {
            val parts = text.split(':').mapNotNull { it.toIntOrNull() }
            return parts.fold(0) { acc, part -> acc * 60 + part }
        }
        val number = text.toDoubleOrNull() ?: return 0
        return if (number > 10_000) (number / 1000).toInt() else number.toInt()
    }

    /** The lyrics in whatever shape the server answered: an LRC string, a JSON string, or timed lines. */
    private fun parsePayload(body: String): String? {
        val json = parseJsonOrNull(body)
        if (json == null) {
            return body.takeIf { it.isNotBlank() && !it.startsWith("{") && !looksLikeError(it) }
        }
        if (json is JsonPrimitive) return json.str()?.trim()?.takeIf { it.isNotBlank() && !looksLikeError(it) }
        if (json["error"] != null && json["lyrics"] == null && json["content"] == null) return null
        for (key in listOf("lrc", "lyrics", "lyric", "lyrics_text", "syncedLyrics", "plain_lyrics", "plainLyrics")) {
            json[key].str()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val lines = (json["content"].arr() ?: json["lyrics"].arr() ?: (json as? JsonArray))
            ?.let(::timedLines)
            ?.withoutCredits()
        return lines?.takeIf { it.hasLyrics() }?.toAppLrc()
    }

    private fun looksLikeError(text: String) =
        text.length < 120 && (text.contains("not found", ignoreCase = true) || text.contains("error", ignoreCase = true))

    /** PaxSenix's timed lines: {timestamp, endtime, text:[{text, timestamp, endtime, part}]} or {time:{total}, text}. */
    private fun timedLines(array: JsonArray): List<TimedLine> = array.mapNotNull { line ->
        val start = line["timestamp"].long()
            ?: line["time"]["total"].double()?.let { (it * 1000).toLong() }
            ?: line["startTime"].long()
            ?: return@mapNotNull null
        val text = line["text"]
        when {
            text is JsonArray -> {
                val parts = text.mapNotNull { word ->
                    val wordText = word["text"].str() ?: return@mapNotNull null
                    val wordStart = word["timestamp"].long()
                    val wordEnd = word["endtime"].long()
                    val joinsNext = (word["part"] as? JsonPrimitive)?.booleanOrNull ?: false
                    Triple(wordText, wordStart to wordEnd, joinsNext)
                }
                val words = if (parts.size > 1 && parts.all { it.second.first != null }) {
                    parts.mapIndexed { index, (wordText, times, joinsNext) ->
                        val isLast = index == parts.lastIndex
                        val spaced = if (!isLast && !joinsNext && !wordText.endsWith(" ")) "$wordText " else wordText
                        val wordStart = times.first!!
                        val wordEnd = times.second ?: parts.getOrNull(index + 1)?.second?.first ?: (wordStart + 300)
                        TimedWord(spaced, wordStart, wordEnd)
                    }
                } else {
                    emptyList()
                }
                val lineText = if (words.isNotEmpty()) words.joinToString("") { it.text }.trim()
                else parts.joinToString(" ") { it.first.trim() }.trim()
                TimedLine(start, lineText, words)
            }
            else -> TimedLine(start, text.str().orEmpty().trim())
        }
    }
}
