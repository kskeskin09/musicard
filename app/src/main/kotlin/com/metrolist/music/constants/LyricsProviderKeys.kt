/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// Switches for the lyrics sources added in Musicard, and for the two YouTube sources that
// used to be always on.
val EnableYouTubeMusicLyricsKey = booleanPreferencesKey("enableYouTubeMusicLyrics")
val EnableYouTubeSubtitleLyricsKey = booleanPreferencesKey("enableYouTubeSubtitleLyrics")
val EnableYouTubeAutoCaptionsKey = booleanPreferencesKey("enableYouTubeAutoCaptions")
val EnableSimpMusicLyricsKey = booleanPreferencesKey("enableSimpMusicLyrics")
val EnableNetEaseLyricsKey = booleanPreferencesKey("enableNetEaseLyrics")
val EnableQQMusicLyricsKey = booleanPreferencesKey("enableQQMusicLyrics")
val EnableMusixmatchLyricsKey = booleanPreferencesKey("enableMusixmatchLyrics")
val EnableSpotifyLyricsKey = booleanPreferencesKey("enableSpotifyLyrics")
val EnableUnisonLyricsKey = booleanPreferencesKey("enableUnisonLyrics")
val EnablePortatoLyricsKey = booleanPreferencesKey("enablePortatoLyrics")
val EnableLrcRedLyricsKey = booleanPreferencesKey("enableLrcRedLyrics")

/** Keep asking the other sources until word-by-word lyrics turn up, and take the best-timed ones. */
val SearchBestSyncedLyricsKey = booleanPreferencesKey("searchBestSyncedLyrics")

/**
 * The source order. A new key, so everyone starts from the new order (most reliable first for
 * English songs) once; it can still be changed on the Lyrics providers page.
 */
val LyricsProviderOrderV2Key = stringPreferencesKey("lyricsProviderOrderV2")
