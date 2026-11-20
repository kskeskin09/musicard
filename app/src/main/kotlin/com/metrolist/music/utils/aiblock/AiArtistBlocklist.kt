/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils.aiblock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.ContentBlocker
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.extensions.metadata
import com.metrolist.music.utils.dataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

val BlockAiArtistsKey = booleanPreferencesKey("blockAiArtists")
private val AiBlocklistUpdatedAtKey = longPreferencesKey("aiArtistBlocklistUpdatedAt")

/**
 * Hides channels on the community AiSList (github.com/Override92/AiSList, CC BY-NC 4.0)
 * from online results and radio/autoplay suggestions. The list is downloaded once a
 * day while the option is on and kept in a file for offline use.
 */
object AiArtistBlocklist {
    const val SOURCE_PAGE = "https://github.com/Override92/AiSList"
    private const val SOURCE_URL = "https://raw.githubusercontent.com/Override92/AiSList/main/AiSList/aislist_blocklist.txt"
    private const val CACHE_FILE = "ai_artist_blocklist.txt"
    private const val MAX_CHARS = 8 * 1024 * 1024
    private const val REFRESH_MS = 24L * 60 * 60 * 1000

    private val channelIdPattern = Regex("UC[A-Za-z0-9_-]{20,24}")

    @Volatile
    private var entries: Set<String> = emptySet()

    @Volatile
    var enabled: Boolean = false
        private set

    val entryCount: Int get() = entries.size

    private val http by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) { requestTimeoutMillis = 30_000 }
            expectSuccess = false
        }
    }

    fun start(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            appContext.dataStore.data
                .map { it[BlockAiArtistsKey] ?: false }
                .distinctUntilChanged()
                .collect { on ->
                    enabled = on
                    if (on) {
                        if (entries.isEmpty()) entries = readCache(appContext)
                        apply()
                        refresh(appContext, force = false)
                    } else {
                        apply()
                    }
                }
        }
    }

    /** Downloads the list if it is older than a day (or always with [force]). Returns false on failure. */
    suspend fun refresh(context: Context, force: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        val now = System.currentTimeMillis()
        val updatedAt = context.dataStore.data.first()[AiBlocklistUpdatedAtKey] ?: 0L
        if (!force && entries.isNotEmpty() && now - updatedAt < REFRESH_MS) return@withContext true
        try {
            val response = http.get(SOURCE_URL)
            check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
            val body = response.bodyAsText()
            check(body.length <= MAX_CHARS) { "list too large" }
            val parsed = parse(body)
            check(parsed.isNotEmpty()) { "empty list" }
            File(context.filesDir, CACHE_FILE).writeText(body)
            entries = parsed
            context.dataStore.edit { it[AiBlocklistUpdatedAtKey] = now }
            apply()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "AI artist blocklist refresh failed; keeping %d cached entries", entries.size)
            false
        }
    }

    private fun readCache(context: Context): Set<String> =
        runCatching { parse(File(context.filesDir, CACHE_FILE).readText()) }.getOrDefault(emptySet())

    private fun apply() {
        ContentBlocker.isBlocked = if (enabled && entries.isNotEmpty()) { item: YTItem -> isBlocked(item) } else null
    }

    fun parse(text: String): Set<String> =
        text.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("@") || it.matches(channelIdPattern) }
            .map(String::lowercase)
            .toHashSet()

    /** Matches a channel id, or a name that equals a listed @handle (with or without its spaces). */
    fun matches(channelId: String?, name: String?): Boolean {
        val list = entries
        if (!enabled || list.isEmpty()) return false
        if (channelId != null && channelId.lowercase() in list) return true
        val n = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
        return "@$n" in list || "@${n.replace(" ", "")}" in list
    }

    fun isBlocked(item: YTItem): Boolean =
        when (item) {
            is SongItem -> item.artists.any { matches(it.id, it.name) }
            is AlbumItem -> item.artists.orEmpty().any { matches(it.id, it.name) }
            is ArtistItem -> matches(item.id, item.title)
            is PlaylistItem -> matches(item.author?.id, item.author?.name)
            is PodcastItem -> matches(item.author?.id, item.author?.name)
            is EpisodeItem -> matches(item.author?.id, item.author?.name)
            else -> false
        }

    fun isBlocked(item: MediaItem): Boolean =
        item.metadata?.artists?.any { matches(it.id, it.name) } == true
}
