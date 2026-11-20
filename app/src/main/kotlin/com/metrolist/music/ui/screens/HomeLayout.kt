/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

/**
 * The home screen sections that can be moved under Content > Edit home screen.
 * The order is saved as comma-separated ids in HomeSectionOrderKey.
 */
object HomeLayout {
    const val QUICK_PICKS = "quick_picks"
    const val ALBUMS_FOR_YOU = "albums_for_you"
    const val RECENTS = "recents"
    const val FORGOTTEN_FAVORITES = "forgotten_favorites"
    const val FROM_THE_COMMUNITY = "from_the_community"
    const val DAILY_DISCOVER = "daily_discover"
    const val KEEP_LISTENING = "keep_listening"
    const val YOUR_MIXES = "account_playlists"
    const val SIMILAR = "similar_recommendations"
    const val YOUTUBE = "youtube_sections"
    const val MOOD_AND_GENRES = "mood_and_genres"
    const val NEW_FOR_YOU = "new_for_you"
    const val ARTIST_SPOTLIGHT = "artist_spotlight"
    const val NEW_FROM_ARTISTS = "new_from_artists"
    const val MOOD_SONGS = "mood_songs"

    /** The order the home screen had before it could be edited. */
    val DefaultOrder =
        listOf(
            QUICK_PICKS,
            NEW_FOR_YOU,
            ALBUMS_FOR_YOU,
            RECENTS,
            ARTIST_SPOTLIGHT,
            FORGOTTEN_FAVORITES,
            MOOD_SONGS,
            FROM_THE_COMMUNITY,
            DAILY_DISCOVER,
            KEEP_LISTENING,
            NEW_FROM_ARTISTS,
            YOUR_MIXES,
            SIMILAR,
            YOUTUBE,
            MOOD_AND_GENRES,
        )

    /**
     * The saved order; unknown ids are dropped. Sections the saved order doesn't know yet (added in
     * a later version) go right after the section they follow by default.
     */
    fun parse(raw: String?): List<String> {
        val saved =
            raw
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it in DefaultOrder }
                .distinct()
        val result = saved.toMutableList()
        DefaultOrder.forEachIndexed { index, id ->
            if (id in result) return@forEachIndexed
            val before = DefaultOrder.subList(0, index).lastOrNull { it in result }
            result.add(if (before == null) 0 else result.indexOf(before) + 1, id)
        }
        return result
    }

    fun serialize(order: List<String>): String = order.joinToString(",")
}
