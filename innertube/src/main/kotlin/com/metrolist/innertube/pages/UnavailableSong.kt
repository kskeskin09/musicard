/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.innertube.pages

/**
 * A playlist row YouTube Music shows greyed out because it can't be played
 * (removed, private or not available in this country). Shown, never played.
 * [afterSongId] is the playable song right before it (null = top of the list).
 */
data class UnavailableSong(
    val title: String,
    val subtitle: String?,
    val thumbnail: String?,
    val afterSongId: String?,
)
