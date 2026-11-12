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

    /** The order the home screen had before it could be edited. */
    val DefaultOrder =
        listOf(
            QUICK_PICKS,
            ALBUMS_FOR_YOU,
            RECENTS,
            FORGOTTEN_FAVORITES,
            FROM_THE_COMMUNITY,
            DAILY_DISCOVER,
            KEEP_LISTENING,
            YOUR_MIXES,
            SIMILAR,
            YOUTUBE,
            MOOD_AND_GENRES,
        )

    /** The saved order; unknown ids are dropped and missing ones keep their default place at the end. */
    fun parse(raw: String?): List<String> {
        val saved =
            raw
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it in DefaultOrder }
                .distinct()
        return saved + DefaultOrder.filter { it !in saved }
    }

    fun serialize(order: List<String>): String = order.joinToString(",")
}
