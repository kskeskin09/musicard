/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics.sources

import com.metrolist.music.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** One HTTP client for the lyrics sources that have no client of their own. */
internal val lyricsHttp: HttpClient by lazy {
    HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 8_000
            socketTimeoutMillis = 12_000
        }
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
        }
        expectSuccess = false
    }
}

internal val lyricsJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

internal fun parseJsonOrNull(text: String): JsonElement? =
    runCatching { lyricsJson.parseToJsonElement(text) }.getOrNull()

internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject

internal fun JsonElement?.arr(): JsonArray? = this as? JsonArray

internal fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull

internal fun JsonElement?.long(): Long? =
    (this as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() ?: it.contentOrNull?.trim()?.toLongOrNull() }

internal fun JsonElement?.double(): Double? =
    (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.trim()?.toDoubleOrNull() }

internal operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)

/** A timed word, in milliseconds. */
internal data class TimedWord(val text: String, val startMs: Long, val endMs: Long)

/** A lyric line, in milliseconds; [words] is empty when only the line is timed. */
internal data class TimedLine(val startMs: Long, val text: String, val words: List<TimedWord> = emptyList())

internal fun lrcTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d.%03d", safe / 60_000, (safe / 1000) % 60, safe % 1000)
}

/**
 * Lines as the app's lyrics text: word-timed lines in the rich format
 * `[mm:ss.xxx]<mm:ss.xxx>word <mm:ss.xxx>word<mm:ss.xxx>`, the others as plain LRC lines.
 */
internal fun List<TimedLine>.toAppLrc(): String = buildString {
    for (line in this@toAppLrc) {
        val words = line.words.filter { it.text.isNotBlank() }
        if (words.isNotEmpty()) {
            append('[').append(lrcTime(line.startMs)).append(']')
            words.forEach { word ->
                append('<').append(lrcTime(word.startMs)).append('>')
                append(word.text.replace('<', '‹').replace('>', '›'))
            }
            append('<').append(lrcTime(words.last().endMs.coerceAtLeast(words.last().startMs))).append('>')
            append('\n')
        } else if (line.text.isNotBlank()) {
            append('[').append(lrcTime(line.startMs)).append(']').append(line.text.trim()).append('\n')
        }
    }
}.trimEnd()

private val LRC_LINE = Regex("""^((?:\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?])+)(.*)$""")
private val LRC_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** Standard LRC (several time tags per line allowed) as lines; metadata tags drop out. */
internal fun parseLrcLines(lrc: String): List<TimedLine> {
    val lines = mutableListOf<TimedLine>()
    lrc.lineSequence().forEach { raw ->
        val match = LRC_LINE.matchEntire(raw.trim()) ?: return@forEach
        val text = match.groupValues[2].trim()
        LRC_TAG.findAll(match.groupValues[1]).forEach { tag ->
            val minutes = tag.groupValues[1].toLong()
            val seconds = tag.groupValues[2].toLong()
            val fraction = tag.groupValues[3]
            val millis = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            lines += TimedLine(minutes * 60_000 + seconds * 1000 + millis, text)
        }
    }
    return lines.sortedBy { it.startMs }
}

/** Credit lines like "作词 : …" or "Lyrics by …" that Chinese services put at the top. */
private val CREDIT_LINE = Regex(
    """^\s*(作词|作曲|编曲|作詞|編曲|制作人|製作人|和声|混音|母带|吉他|贝斯|鼓|录音|监制|出品|发行|OP|SP|Lyricist|Composer|Arranger|Producer|Lyrics by|Composed by|Written by)\s*[:：]""",
    RegexOption.IGNORE_CASE,
)

internal fun List<TimedLine>.withoutCredits(): List<TimedLine> =
    filterNot { CREDIT_LINE.containsMatchIn(it.text) || it.text.isBlank() && it.words.isEmpty() }

internal fun List<TimedLine>.hasLyrics(): Boolean = count { it.text.isNotBlank() || it.words.isNotEmpty() } >= 2

/** TTML (Apple Music style) as the app's lyrics text, or null when it has no lines. */
internal fun ttmlToAppLrc(ttml: String): String? =
    runCatching {
        val parsed = TTMLParser.parseTTML(ttml)
        if (parsed.isEmpty()) null else TTMLParser.toLRC(parsed).takeIf { it.isNotBlank() }
    }.getOrNull()

// ---- picking the right recording from search results ----

internal data class TrackCandidate<T>(
    val title: String,
    val artists: List<String>,
    val durationSec: Int,
    val ref: T,
)

private val VERSION_WORDS = setOf(
    "live", "remix", "mix", "acoustic", "unplugged", "instrumental", "karaoke", "acapella", "cappella",
    "demo", "cover", "sped", "slowed", "reverb", "nightcore", "lofi", "extended", "orchestral",
)
private val BRACKETED = Regex("""[(\[（【][^)\]）】]*[)\]）】]""")

internal fun normalizeForMatch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .trim()

private fun coreTitle(title: String): String =
    normalizeForMatch(title.replace(BRACKETED, " ").substringBefore(" - ")).ifBlank { normalizeForMatch(title) }

private fun versionWords(title: String): Set<String> =
    normalizeForMatch(title).split(' ').filter { it in VERSION_WORDS }.toSet()

private val ARTIST_SPLIT = Regex("""\s*(?:,|&|;|/|、|\band\b|\bx\b|\bwith\b|\bfeat\.?|\bft\.?)\s*""", RegexOption.IGNORE_CASE)

internal fun splitArtists(artist: String): List<String> =
    artist.split(ARTIST_SPLIT).map { normalizeForMatch(it) }.filter { it.isNotBlank() }

/** The first artist only, for services that search better without the features. */
internal fun primaryArtist(artist: String): String =
    artist.split(ARTIST_SPLIT).firstOrNull()?.trim().takeUnless { it.isNullOrBlank() } ?: artist

/**
 * The candidate that is this recording: titles must match (ignoring brackets), the version words
 * (live, remix…) must agree, an artist should match, and the closest length wins.
 */
internal fun <T> List<TrackCandidate<T>>.bestMatch(
    title: String,
    artist: String,
    durationSec: Int,
    maxDurationDiff: Int = 6,
): TrackCandidate<T>? {
    val wantedTitle = coreTitle(title)
    val wantedVersion = versionWords(title)
    val wantedArtists = splitArtists(artist).toSet()
    return asSequence()
        .mapNotNull { candidate ->
            val candidateTitle = coreTitle(candidate.title)
            val titleScore = when {
                candidateTitle == wantedTitle -> 100
                candidateTitle.isNotBlank() && wantedTitle.isNotBlank() &&
                    (candidateTitle.contains(wantedTitle) || wantedTitle.contains(candidateTitle)) -> 50
                else -> return@mapNotNull null
            }
            if (versionWords(candidate.title) != wantedVersion) return@mapNotNull null
            val candidateArtists = candidate.artists.flatMap { splitArtists(it) }.toSet()
            val artistScore = when {
                wantedArtists.isEmpty() -> 0
                candidateArtists.any { it in wantedArtists } -> 60
                candidateArtists.any { c -> wantedArtists.any { w -> c.contains(w) || w.contains(c) } } -> 30
                else -> -40
            }
            val diff = if (durationSec > 0 && candidate.durationSec > 0) abs(candidate.durationSec - durationSec) else 0
            if (durationSec > 0 && candidate.durationSec > 0 && diff > maxDurationDiff) return@mapNotNull null
            val score = titleScore + artistScore - diff * 3
            if (score < 60) null else candidate to score
        }.maxByOrNull { it.second }
        ?.first
}
