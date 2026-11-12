/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.RecentPlaylistsKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Playlists played lately, newest first, as (playlist id, time played in ms). */
fun parseRecentPlaylists(raw: String): List<Pair<String, Long>> =
    raw.lines().mapNotNull { line ->
        val id = line.substringBefore('|', "")
        val time = line.substringAfter('|', "").toLongOrNull()
        if (id.isBlank() || time == null) null else id to time
    }

/** Remembers that a playlist was just played (for the recently played row on Home). */
fun Context.markPlaylistPlayed(playlistId: String) {
    val appContext = applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        appContext.dataStore.edit { prefs ->
            val others = parseRecentPlaylists(prefs[RecentPlaylistsKey].orEmpty()).filter { it.first != playlistId }
            prefs[RecentPlaylistsKey] =
                (listOf(playlistId to System.currentTimeMillis()) + others)
                    .take(20)
                    .joinToString("\n") { "${it.first}|${it.second}" }
        }
    }
}
