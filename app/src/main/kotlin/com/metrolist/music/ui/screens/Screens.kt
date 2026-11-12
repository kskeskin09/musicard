/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.metrolist.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    // Placeholder tab; its content will be added later.
    object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.grid_view,
        iconIdActive = R.drawable.grid_view,
        route = "explore_tab"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    // Profile picture, history, stats, Listen Together and the account settings.
    object Profile : Screens(
        titleId = R.string.profile_tab,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "profile_tab"
    )

    companion object {
        val MainScreens = listOf(Home, Search, ListenTogether, Library, Profile)
    }
}
