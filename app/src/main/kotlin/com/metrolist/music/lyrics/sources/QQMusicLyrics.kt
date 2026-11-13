/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics.sources

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

/**
 * QQ Music, asked without an account. Strong on Chinese and other Asian songs; the lyrics are
 * timed per line (QQ's word timing is sent encrypted, so it isn't read here).
 */
internal object QQMusicLyrics {
    private const val ENDPOINT = "https://u.y.qq.com/cgi-bin/musicu.fcg"
    private const val SEARCH_LIMIT = 10

    private data class Song(val mid: String, val id: Long)

    suspend fun getLyrics(title: String, artist: String, durationSec: Int): Result<String> = runCatching {
        val candidates = search("$title ${primaryArtist(artist)}").ifEmpty { search(title) }
        val song = candidates.bestMatch(title, artist, durationSec)?.ref
            ?: throw IllegalStateException("No matching QQ Music song")
        val encoded = musicu(
            module = "music.musichallSong.PlayLyricInfo",
            method = "GetPlayLyricInfo",
            param = buildJsonObject {
                put("songMID", song.mid)
                put("songID", song.id)
            },
        )["req"]["data"]["lyric"].str().orEmpty()
        if (encoded.isBlank()) throw IllegalStateException("QQ Music has no lyrics for this song")
        val lrc = runCatching { String(Base64.getDecoder().decode(encoded), Charsets.UTF_8) }.getOrDefault(encoded)
        val lines = parseLrcLines(lrc).withoutCredits()
        if (!lines.hasLyrics()) throw IllegalStateException("QQ Music has no lyrics for this song")
        lines.toAppLrc()
    }

    private suspend fun search(query: String): List<TrackCandidate<Song>> {
        val list = musicu(
            module = "music.search.SearchCgiService",
            method = "DoSearchForQQMusicDesktop",
            param = buildJsonObject {
                put("query", query)
                put("num_per_page", SEARCH_LIMIT)
                put("page_num", 1)
                put("search_type", 0)
            },
        )["req"]["data"]["body"]["song"]["list"].arr() ?: return emptyList()
        return list.mapNotNull { song ->
            val mid = song["mid"].str() ?: return@mapNotNull null
            val id = song["id"].long() ?: return@mapNotNull null
            TrackCandidate(
                title = song["name"].str() ?: song["title"].str() ?: return@mapNotNull null,
                artists = song["singer"].arr()?.mapNotNull { it["name"].str() }.orEmpty(),
                durationSec = song["interval"].long()?.toInt() ?: 0,
                ref = Song(mid, id),
            )
        }
    }

    // Every QQ Music call goes through this one endpoint, named by a module and a method.
    private suspend fun musicu(module: String, method: String, param: JsonObject): JsonElement? {
        val body = buildJsonObject {
            put("comm", buildJsonObject {
                put("ct", 19)
                put("cv", 1859)
            })
            put("req", buildJsonObject {
                put("module", module)
                put("method", method)
                put("param", param)
            })
        }
        val response = lyricsHttp.post(ENDPOINT) {
            header("Referer", "https://y.qq.com/")
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) return null
        return parseJsonOrNull(response.bodyAsText())
    }
}
