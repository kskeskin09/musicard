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
 * NetEase Cloud Music, asked without an account. Strong on Chinese, Japanese and Korean songs,
 * and many songs (English ones too) come with word-by-word timing ("yrc").
 */
internal object NetEaseLyrics {
    private const val BASE = "https://music.163.com"
    private const val SEARCH_LIMIT = 10

    suspend fun getLyrics(title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        val candidates = search("$title ${primaryArtist(artist)}").ifEmpty { search(title) }
        val song = candidates.bestMatch(title, artist, durationSec)
            ?: throw IllegalStateException("No matching NetEase song")
        lyricsFor(song.ref) ?: throw IllegalStateException("NetEase has no lyrics for this song")
    }

    private suspend fun search(query: String): List<TrackCandidate<Long>> {
        val response = lyricsHttp.get("$BASE/api/search/get") {
            header("Referer", "$BASE/")
            parameter("s", query)
            parameter("type", 1)
            parameter("limit", SEARCH_LIMIT)
        }
        if (!response.status.isSuccess()) return emptyList()
        val songs = parseJsonOrNull(response.bodyAsText())["result"]["songs"].arr() ?: return emptyList()
        return songs.mapNotNull { song ->
            val id = song["id"].long() ?: return@mapNotNull null
            val name = song["name"].str() ?: return@mapNotNull null
            TrackCandidate(
                title = name,
                artists = song["artists"].arr()?.mapNotNull { it["name"].str() }.orEmpty(),
                durationSec = ((song["duration"].long() ?: 0L) / 1000).toInt(),
                ref = id,
            )
        }
    }

    private suspend fun lyricsFor(id: Long): String? {
        val response = lyricsHttp.get("$BASE/api/song/lyric") {
            header("Referer", "$BASE/")
            parameter("id", id)
            parameter("lv", 1)
            parameter("yv", 1)
            parameter("tv", -1)
        }
        if (!response.status.isSuccess()) return null
        val root = parseJsonOrNull(response.bodyAsText()) ?: return null
        val wordTimed = root["yrc"]["lyric"].str()?.let(::parseYrc)?.withoutCredits()
        if (wordTimed != null && wordTimed.hasLyrics()) return wordTimed.toAppLrc()
        val lineTimed = root["lrc"]["lyric"].str()?.let(::parseLrcLines)?.withoutCredits()
        if (lineTimed != null && lineTimed.hasLyrics()) return lineTimed.toAppLrc()
        return null
    }

    private val YRC_LINE = Regex("""^\[(\d+),(\d+)](.*)$""")
    // A word runs until the next "(start,length,0)" marker, so brackets inside words survive.
    private val YRC_WORD = Regex("""\((\d+),(\d+),-?\d+\)(.*?)(?=\(\d+,\d+,-?\d+\)|$)""")

    /** "[lineStart,lineLength](wordStart,wordLength,0)word…" in milliseconds; JSON credit lines are skipped. */
    private fun parseYrc(yrc: String): List<TimedLine> =
        yrc.lineSequence().mapNotNull { raw ->
            val line = YRC_LINE.matchEntire(raw.trim()) ?: return@mapNotNull null
            val words = YRC_WORD.findAll(line.groupValues[3]).map { word ->
                val start = word.groupValues[1].toLong()
                TimedWord(word.groupValues[3], start, start + word.groupValues[2].toLong())
            }.filter { it.text.isNotEmpty() }.toList()
            if (words.isEmpty()) return@mapNotNull null
            TimedLine(line.groupValues[1].toLong(), words.joinToString("") { it.text }.trim(), words)
        }.toList()
}
