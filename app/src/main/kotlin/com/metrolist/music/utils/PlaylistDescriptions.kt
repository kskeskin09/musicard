/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The descriptions written for your own playlists, kept on the device by playlist id. */
object PlaylistDescriptions {
    private const val PREFS = "playlist_descriptions"
    private val descriptions = MutableStateFlow<Map<String, String>>(emptyMap())

    @Volatile
    private var loaded = false

    private fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            descriptions.value = prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
            loaded = true
        }
    }

    fun all(context: Context): StateFlow<Map<String, String>> {
        load(context)
        return descriptions.asStateFlow()
    }

    fun get(
        context: Context,
        playlistId: String,
    ): String? {
        load(context)
        return descriptions.value[playlistId]
    }

    fun set(
        context: Context,
        playlistId: String,
        description: String?,
    ) {
        load(context)
        val text = description?.trim().orEmpty()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (text.isEmpty()) {
            prefs.edit().remove(playlistId).apply()
            descriptions.value = descriptions.value - playlistId
        } else {
            prefs.edit().putString(playlistId, text).apply()
            descriptions.value = descriptions.value + (playlistId to text)
        }
    }
}

/** The description of the playlist [playlistId], or null when it has none. */
@Composable
fun rememberPlaylistDescription(playlistId: String?): String? {
    val context = LocalContext.current
    val flow = remember(context) { PlaylistDescriptions.all(context) }
    val all by flow.collectAsStateWithLifecycle()
    return playlistId?.let { all[it] }?.takeIf { it.isNotBlank() }
}
