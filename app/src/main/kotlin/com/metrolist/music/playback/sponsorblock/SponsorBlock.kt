/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.sponsorblock

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.security.MessageDigest

/** Podcast ad categories, as used by Metrolist. */
val SponsorBlockPodcastCategories = setOf("sponsor", "selfpromo", "interaction")

/** Intros, talking and other non-music parts of music videos. */
const val SponsorBlockMusicCategory = "music_offtopic"

data class SponsorBlockSegment(val startMs: Long, val endMs: Long)

/**
 * Parses a `skipSegments/<hashPrefix>` answer, keeping only [videoId], the given
 * [categories] and "skip" segments, merged where they overlap.
 */
fun parseSponsorBlockSegments(
    body: String,
    videoId: String,
    categories: Set<String>,
): List<SponsorBlockSegment> {
    val videos = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonArray ?: return emptyList()
    val video = videos.filterIsInstance<JsonObject>()
        .firstOrNull { (it["videoID"] as? JsonPrimitive)?.contentOrNull == videoId }
    val segments = video?.get("segments") as? JsonArray ?: return emptyList()
    val valid = segments.mapNotNull { element ->
        val segment = element as? JsonObject ?: return@mapNotNull null
        if ((segment["category"] as? JsonPrimitive)?.contentOrNull !in categories) return@mapNotNull null
        if ((segment["actionType"] as? JsonPrimitive)?.contentOrNull != "skip") return@mapNotNull null
        val range = segment["segment"] as? JsonArray ?: return@mapNotNull null
        if (range.size != 2) return@mapNotNull null
        val start = (range[0] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        val end = (range[1] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        if (!start.isFinite() || !end.isFinite() || start < 0 || end <= start || end > 1e9) return@mapNotNull null
        SponsorBlockSegment((start * 1000).toLong(), (end * 1000).toLong()).takeIf { it.endMs > it.startMs }
    }.sortedBy { it.startMs }
    val merged = mutableListOf<SponsorBlockSegment>()
    for (segment in valid) {
        val previous = merged.lastOrNull()
        if (previous != null && segment.startMs <= previous.endMs) {
            merged[merged.lastIndex] = previous.copy(endMs = maxOf(previous.endMs, segment.endMs))
        } else {
            merged += segment
        }
    }
    return merged
}

/** Where to jump when [positionMs] is inside a segment, or null. Segments that run past the end are ignored. */
fun List<SponsorBlockSegment>.skipTarget(positionMs: Long, durationMs: Long): Long? =
    firstOrNull {
        positionMs >= it.startMs && positionMs < it.endMs - 300 && (durationMs <= 0 || it.endMs < durationMs - 500)
    }?.endMs

object SponsorBlockClient {
    private val http: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 6_000
                connectTimeoutMillis = 5_000
            }
            expectSuccess = false
        }
    }

    private data class Cached(val segments: List<SponsorBlockSegment>, val fetchedAt: Long)

    private val cache = LinkedHashMap<String, Cached>()
    private val mutex = Mutex()

    suspend fun segments(videoId: String, categories: Set<String>): List<SponsorBlockSegment> {
        if (categories.isEmpty() || videoId.isBlank() || videoId.startsWith("local_")) return emptyList()
        val key = videoId + "|" + categories.sorted().joinToString(",")
        return mutex.withLock {
            val now = System.currentTimeMillis()
            cache[key]?.let {
                val lifetime = if (it.segments.isEmpty()) 5 * 60_000L else 60 * 60_000L
                if (now - it.fetchedAt < lifetime) return@withLock it.segments
                cache.remove(key)
            }
            val segments = try {
                // Hash prefix lookup: the server never sees which video is playing.
                val prefix = MessageDigest.getInstance("SHA-256")
                    .digest(videoId.toByteArray())
                    .take(2)
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                val response = http.get("https://sponsor.ajay.app/api/skipSegments/$prefix") {
                    categories.forEach { parameter("category", it) }
                    parameter("actionType", "skip")
                }
                if (response.status == HttpStatusCode.OK) {
                    parseSponsorBlockSegments(response.bodyAsText(), videoId, categories)
                } else {
                    emptyList()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            if (cache.size >= 64) cache.remove(cache.keys.first())
            cache[key] = Cached(segments, now)
            segments
        }
    }
}
