/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.constants.RecentSearchContentsKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** How many opened search results "Recent contents" shows on the empty search screen. */
const val RECENT_SEARCH_CONTENTS_SHOWN = 3
private const val RECENT_SEARCH_CONTENTS_KEPT = 10

private fun JSONObject.str(name: String): String? = optString(name).takeIf { has(name) && it.isNotEmpty() }

private fun artistsToJson(artists: List<Artist>?) =
    JSONArray().apply {
        artists.orEmpty().forEach { a -> put(JSONObject().put("name", a.name).put("id", a.id ?: "")) }
    }

private fun artistsFromJson(array: JSONArray?): List<Artist> =
    (0 until (array?.length() ?: 0)).mapNotNull { i ->
        val o = array!!.optJSONObject(i) ?: return@mapNotNull null
        Artist(o.optString("name"), o.str("id"))
    }

/** Only what is needed to show the item in a list and open it again. */
private fun YTItem.toRecentJson(): JSONObject? {
    val o = JSONObject().put("id", id).put("title", title).put("thumbnail", thumbnail ?: "")
    return when (this) {
        is SongItem ->
            o
                .put("type", "song")
                .put("artists", artistsToJson(artists))
                .put("videoType", musicVideoType ?: "")
                .put("albumName", album?.name ?: "")
                .put("albumId", album?.id ?: "")
                .put("explicit", explicit)
        is EpisodeItem -> asSongItem().toRecentJson()
        is AlbumItem ->
            o
                .put("type", "album")
                .put("playlistId", playlistId)
                .put("artists", artistsToJson(artists))
                .put("year", year ?: 0)
                .put("explicit", explicit)
        is ArtistItem -> o.put("type", "artist")
        is PlaylistItem ->
            o
                .put("type", "playlist")
                .put("author", author?.name ?: "")
                .put("authorId", author?.id ?: "")
        is PodcastItem ->
            o
                .put("type", "podcast")
                .put("author", author?.name ?: "")
                .put("authorId", author?.id ?: "")
    }
}

private fun recentFromJson(o: JSONObject): YTItem? {
    val id = o.str("id") ?: return null
    val title = o.str("title") ?: return null
    val thumbnail = o.str("thumbnail")
    return when (o.optString("type")) {
        "song" ->
            SongItem(
                id = id,
                title = title,
                artists = artistsFromJson(o.optJSONArray("artists")),
                album =
                    o.str("albumId")?.let { albumId ->
                        com.metrolist.innertube.models.Album(o.optString("albumName"), albumId)
                    },
                musicVideoType = o.str("videoType"),
                thumbnail = thumbnail ?: return null,
                explicit = o.optBoolean("explicit"),
            )
        "album" ->
            AlbumItem(
                browseId = id,
                playlistId = o.str("playlistId") ?: return null,
                title = title,
                artists = artistsFromJson(o.optJSONArray("artists")),
                year = o.optInt("year").takeIf { it > 0 },
                thumbnail = thumbnail ?: return null,
                explicit = o.optBoolean("explicit"),
            )
        "artist" ->
            ArtistItem(
                id = id,
                title = title,
                thumbnail = thumbnail,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        "playlist" ->
            PlaylistItem(
                id = id,
                title = title,
                author = o.str("author")?.let { Artist(it, o.str("authorId")) },
                songCountText = null,
                thumbnail = thumbnail,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        "podcast" ->
            PodcastItem(
                id = id,
                title = title,
                author = o.str("author")?.let { Artist(it, o.str("authorId")) },
                episodeCountText = null,
                thumbnail = thumbnail,
                playEndpoint = null,
                shuffleEndpoint = null,
            )
        else -> null
    }
}

private fun parseRecentRaw(raw: String?): List<JSONObject> =
    try {
        val array = JSONArray(raw.orEmpty().ifBlank { "[]" })
        (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    } catch (e: Exception) {
        emptyList()
    }

/** Items opened from search lately, newest first. */
fun Context.recentSearchContents(): Flow<List<YTItem>> =
    dataStore.data.map { prefs ->
        parseRecentRaw(prefs[RecentSearchContentsKey]).mapNotNull { recentFromJson(it) }
    }

/** Remembers that [item] was opened from search (it moves to the top of "Recent contents"). */
fun Context.markSearchContentOpened(item: YTItem) {
    val json = item.toRecentJson() ?: return
    val appContext = applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        appContext.safeDataStoreEdit { prefs ->
            val others = parseRecentRaw(prefs[RecentSearchContentsKey]).filter { it.optString("id") != item.id }
            prefs[RecentSearchContentsKey] =
                JSONArray((listOf(json) + others).take(RECENT_SEARCH_CONTENTS_KEPT)).toString()
        }
    }
}

/** Removes one item from "Recent contents". */
fun Context.removeRecentSearchContent(id: String) {
    val appContext = applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        appContext.safeDataStoreEdit { prefs ->
            prefs[RecentSearchContentsKey] =
                JSONArray(parseRecentRaw(prefs[RecentSearchContentsKey]).filter { it.optString("id") != id }).toString()
        }
    }
}
